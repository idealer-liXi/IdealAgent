package com.idealagent.infrastructure.rag;

import com.idealagent.domain.rag.model.entity.RagChunk;
import com.idealagent.domain.rag.repository.IRagRepository;
import com.idealagent.domain.rag.service.RagException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.util.List;

@Repository
public class PgVectorRagRepository implements IRagRepository {
    private final JdbcTemplate jdbcTemplate;
    private final String tableName;

    public PgVectorRagRepository(
            @Value("${ideal-agent.rag.pgvector.url:jdbc:postgresql://127.0.0.1:15432/vector_store}") String url,
            @Value("${ideal-agent.rag.pgvector.username:postgres}") String username,
            @Value("${ideal-agent.rag.pgvector.password:postgres}") String password,
            @Value("${ideal-agent.rag.pgvector.table-name:public.vector_store_openai}") String tableName) {
        this.jdbcTemplate = new JdbcTemplate(dataSource(url, username, password));
        this.tableName = tableName;
    }

    @Override
    public void saveChunks(Long userId, String ragTag, List<RagChunk> chunks) {
        String sql = "INSERT INTO " + tableName + " (content, metadata, embedding) VALUES (?, ?::jsonb, ?::vector)";
        try {
            for (RagChunk chunk : chunks) {
                jdbcTemplate.update(sql, chunk.content(), metadata(userId, ragTag, chunk.source()), vector(chunk.embedding()));
            }
        } catch (DataAccessException e) {
            throw unavailable(e);
        }
    }

    @Override
    public List<String> listTags(Long userId) {
        String sql = "SELECT DISTINCT metadata::jsonb->>'knowledge' AS knowledge FROM " + tableName
                + " WHERE metadata::jsonb->>'userId' = ? AND metadata::jsonb->>'knowledge' <> '' ORDER BY knowledge";
        try {
            return jdbcTemplate.queryForList(sql, String.class, String.valueOf(userId));
        } catch (DataAccessException e) {
            throw unavailable(e);
        }
    }

    @Override
    public List<RagChunk> search(Long userId, String ragTag, float[] queryEmbedding, int limit) {
        String sql = "SELECT content, metadata::jsonb->>'source' AS source FROM " + tableName
                + " WHERE metadata::jsonb->>'userId' = ? AND metadata::jsonb->>'knowledge' = ?"
                + " ORDER BY embedding <=> ?::vector LIMIT ?";
        try {
            return jdbcTemplate.query(sql, (rs, rowNum) -> new RagChunk(rs.getString("content"), rs.getString("source"), new float[0]),
                    String.valueOf(userId), ragTag, vector(queryEmbedding), limit);
        } catch (DataAccessException e) {
            throw unavailable(e);
        }
    }

    private RagException unavailable(DataAccessException e) {
        return new RagException("知识库服务不可用，请检查 pgvector 配置", e);
    }

    private DataSource dataSource(String url, String username, String password) {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.postgresql.Driver");
        dataSource.setUrl(url);
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        return dataSource;
    }

    private String metadata(Long userId, String ragTag, String source) {
        return "{\"knowledge\":\"" + escape(ragTag) + "\",\"userId\":\"" + userId + "\",\"source\":\"" + escape(source) + "\"}";
    }

    private String escape(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private String vector(float[] embedding) {
        if (embedding == null || embedding.length == 0) {
            throw new RagException("向量不能为空");
        }
        StringBuilder builder = new StringBuilder("[");
        for (int i = 0; i < embedding.length; i++) {
            if (i > 0) {
                builder.append(',');
            }
            builder.append(embedding[i]);
        }
        return builder.append(']').toString();
    }
}
