package com.idealagent.infrastructure.rag;

import com.idealagent.domain.ai.repository.IRagTagRepository;
import com.idealagent.domain.ai.service.rag.RagException;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

public class PgVectorRagTagRepository implements IRagTagRepository {
    private final JdbcTemplate jdbcTemplate;
    private final String tableName;

    public PgVectorRagTagRepository(JdbcTemplate jdbcTemplate, String tableName) {
        this.jdbcTemplate = jdbcTemplate;
        this.tableName = tableName;
    }

    @Override
    public List<String> listTags(Long userId) {
        String sql = "SELECT DISTINCT metadata::jsonb->>'knowledge' AS knowledge FROM " + tableName
                + " WHERE metadata::jsonb->>'userId' = ? AND metadata::jsonb->>'knowledge' <> '' ORDER BY knowledge";
        try {
            return jdbcTemplate.queryForList(sql, String.class, String.valueOf(userId));
        } catch (DataAccessException e) {
            throw new RagException("知识库服务不可用，请检查 pgvector 配置", e);
        }
    }
}
