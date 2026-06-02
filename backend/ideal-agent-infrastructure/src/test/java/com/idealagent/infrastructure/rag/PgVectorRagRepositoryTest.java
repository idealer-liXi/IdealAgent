package com.idealagent.infrastructure.rag;

import com.idealagent.domain.rag.service.RagException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PgVectorRagRepositoryTest {
    @Test
    void listTagsWrapsUnavailablePgVectorAsRagException() {
        PgVectorRagRepository repository = new PgVectorRagRepository(
                "jdbc:postgresql://127.0.0.1:1/missing",
                "postgres",
                "postgres",
                "public.vector_store_openai");

        RagException exception = assertThrows(RagException.class, () -> repository.listTags(7L));

        assertEquals("知识库服务不可用，请检查 pgvector 配置", exception.getMessage());
    }
}
