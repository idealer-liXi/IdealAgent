package com.idealagent.domain.ai.service.rag;

import com.idealagent.domain.ai.model.dto.RagUploadDTO;
import com.idealagent.domain.ai.model.entity.RagChunk;
import com.idealagent.domain.ai.model.entity.RagFile;
import com.idealagent.domain.ai.model.vo.RagTagVO;
import com.idealagent.domain.ai.repository.IRagRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RagServiceTest {
    private FakeRagRepository repository;
    private RagService service;

    @BeforeEach
    void setUp() {
        repository = new FakeRagRepository();
        service = new RagService(repository, new SimpleTextSplitter(), new DeterministicEmbeddingService(), null);
    }

    @Test
    void uploadFilesSplitsEmbedsAndStoresChunks() {
        service.uploadFiles(7L, "spring-ai", List.of(new RagFile("note.md", "Spring AI supports vector search.\npgvector stores embeddings.")));

        assertThat(repository.savedTag).isEqualTo("spring-ai");
        assertThat(repository.savedUserId).isEqualTo(7L);
        assertThat(repository.savedChunks).isNotEmpty();
        assertThat(repository.savedChunks.get(0).embedding()).hasSize(1024);
    }

    @Test
    void listTagsReturnsUserKnowledgeTags() {
        repository.tags = List.of("spring-ai", "pgvector");

        List<RagTagVO> tags = service.listTags(7L);

        assertThat(tags).extracting(RagTagVO::ragTag).containsExactly("spring-ai", "pgvector");
    }

    @Test
    void retrieveUsesQueryEmbeddingAndRepositorySearch() {
        repository.searchResults = List.of(new RagChunk("pgvector stores vectors", "note.md", new float[1024]));

        List<RagChunk> chunks = service.retrieve(7L, "spring-ai", "How to store vectors?");

        assertThat(repository.searchTag).isEqualTo("spring-ai");
        assertThat(repository.searchEmbedding).hasSize(1024);
        assertThat(chunks).extracting(RagChunk::content).containsExactly("pgvector stores vectors");
    }

    @Test
    void uploadRejectsBlankTag() {
        assertThatThrownBy(() -> service.uploadFiles(7L, " ", List.of(new RagFile("note.md", "text"))))
                .isInstanceOf(RagException.class)
                .hasMessage("知识库标签不能为空");
    }

    @Test
    void embeddingIsDeterministicAndFixedSize() {
        DeterministicEmbeddingService embeddingService = new DeterministicEmbeddingService();

        assertThat(embeddingService.embed("same text")).containsExactly(embeddingService.embed("same text"));
        assertThat(embeddingService.embed("same text")).hasSize(1024);
    }

    private static class FakeRagRepository implements IRagRepository {
        private Long savedUserId;
        private String savedTag;
        private List<RagChunk> savedChunks = new ArrayList<>();
        private List<String> tags = new ArrayList<>();
        private String searchTag;
        private float[] searchEmbedding;
        private List<RagChunk> searchResults = new ArrayList<>();

        @Override
        public void saveChunks(Long userId, String ragTag, List<RagChunk> chunks) {
            savedUserId = userId;
            savedTag = ragTag;
            savedChunks = chunks;
        }

        @Override
        public List<String> listTags(Long userId) {
            return tags;
        }

        @Override
        public List<RagChunk> search(Long userId, String ragTag, float[] queryEmbedding, int limit) {
            searchTag = ragTag;
            searchEmbedding = queryEmbedding;
            return searchResults;
        }
    }
}
