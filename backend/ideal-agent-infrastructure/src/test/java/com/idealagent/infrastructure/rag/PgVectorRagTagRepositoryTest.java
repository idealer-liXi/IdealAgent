package com.idealagent.infrastructure.rag;

import com.idealagent.domain.ai.service.rag.RagException;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PgVectorRagTagRepositoryTest {
    @Test
    void listTagsQueriesDistinctKnowledgeTagsForUserOrderedByKnowledge() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForList(any(String.class), eq(String.class), eq("7")))
                .thenReturn(List.of("pgvector", "spring-ai"));
        PgVectorRagTagRepository repository = new PgVectorRagTagRepository(jdbcTemplate, "public.vector_store_openai");

        List<String> tags = repository.listTags(7L);

        assertEquals(List.of("pgvector", "spring-ai"), tags);
        verify(jdbcTemplate).queryForList(
                org.mockito.ArgumentMatchers.contains("metadata::jsonb->>'knowledge'"),
                eq(String.class),
                eq("7"));
        verify(jdbcTemplate).queryForList(
                org.mockito.ArgumentMatchers.contains("metadata::jsonb->>'userId' = ?"),
                eq(String.class),
                eq("7"));
        verify(jdbcTemplate).queryForList(
                org.mockito.ArgumentMatchers.contains("ORDER BY knowledge"),
                eq(String.class),
                eq("7"));
    }

    @Test
    void listTagsWrapsUnavailablePgVectorAsRagException() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.postgresql.Driver");
        dataSource.setUrl("jdbc:postgresql://127.0.0.1:1/missing");
        dataSource.setUsername("postgres");
        dataSource.setPassword("postgres");
        PgVectorRagTagRepository repository = new PgVectorRagTagRepository(new JdbcTemplate(dataSource), "public.vector_store_openai");

        RagException exception = assertThrows(RagException.class, () -> repository.listTags(7L));

        assertEquals("知识库服务不可用，请检查 pgvector 配置", exception.getMessage());
    }
}
