package com.idealagent.domain.ai.service.augment;

import com.idealagent.domain.ai.model.entity.RagChunk;
import com.idealagent.domain.ai.service.rag.RagService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.Message;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AugmentServiceTest {
    private RagService ragService;
    private AugmentService service;

    @BeforeEach
    void setUp() {
        ragService = mock(RagService.class);
        service = new AugmentService(ragService);
    }

    @Test
    void augmentRagMessageReturnsUserMessageWithoutRagTag() {
        List<Message> messages = service.augmentRagMessage(7L, "hello", null);

        assertThat(messages).hasSize(1);
        assertThat(messages.get(0).getText()).isEqualTo("hello");
        verifyNoInteractions(ragService);
    }

    @Test
    void augmentRagMessageAddsSystemContextFromRetrievedChunks() {
        when(ragService.retrieve(7L, "spring-ai", "How?"))
                .thenReturn(List.of(new RagChunk("pgvector stores vectors", "note.md", new float[0])));

        List<Message> messages = service.augmentRagMessage(7L, "How?", "spring-ai");

        assertThat(messages).hasSize(2);
        assertThat(messages.get(0).getText()).contains("DOCUMENTS").contains("pgvector stores vectors");
        assertThat(messages.get(1).getText()).isEqualTo("How?");
        verify(ragService).retrieve(7L, "spring-ai", "How?");
    }

    @Test
    void augmentRagMessageUsesConfiguredTopKWhenProvided() {
        when(ragService.retrieve(7L, "spring-ai", "How?", 4))
                .thenReturn(List.of(new RagChunk("top k context", "note.md", new float[0])));

        List<Message> messages = service.augmentRagMessage(7L, "How?", "spring-ai", 4);

        assertThat(messages).hasSize(2);
        assertThat(messages.get(0).getText()).contains("top k context");
        verify(ragService).retrieve(7L, "spring-ai", "How?", 4);
    }

    @Test
    void augmentRagMessagePassesFilterExpressionWhenProvided() {
        when(ragService.retrieve(7L, "spring-ai", "How?", 4, "source == 'note.md'"))
                .thenReturn(List.of(new RagChunk("filtered context", "note.md", new float[0])));

        List<Message> messages = service.augmentRagMessage(7L, "How?", "spring-ai", 4, "source == 'note.md'");

        assertThat(messages).hasSize(2);
        assertThat(messages.get(0).getText()).contains("filtered context");
        verify(ragService).retrieve(7L, "spring-ai", "How?", 4, "source == 'note.md'");
    }
}
