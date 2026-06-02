package com.idealagent.infrastructure.rag;

import com.idealagent.domain.ai.service.rag.RagException;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PgVectorRagRepositoryTest {
    @Test
    void listTagsWrapsUnavailablePgVectorAsRagException() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.postgresql.Driver");
        dataSource.setUrl("jdbc:postgresql://127.0.0.1:1/missing");
        dataSource.setUsername("postgres");
        dataSource.setPassword("postgres");
        PgVectorRagRepository repository = new PgVectorRagRepository(new JdbcTemplate(dataSource), "public.vector_store_openai");

        RagException exception = assertThrows(RagException.class, () -> repository.listTags(7L));

        assertEquals("知识库服务不可用，请检查 pgvector 配置", exception.getMessage());
    }
}
