package com.idealagent.domain.ai.service.rag;

import com.idealagent.domain.ai.model.dto.RagUploadDTO;
import com.idealagent.domain.ai.model.entity.RagChunk;
import com.idealagent.domain.ai.model.entity.RagFile;
import com.idealagent.domain.ai.model.vo.RagTagVO;
import com.idealagent.domain.ai.repository.IRagTagRepository;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RagServiceTest {
    private FakeRagVectorStore vectorStore;
    private FakeRagTagRepository tagRepository;
    private RagService service;

    @BeforeEach
    void setUp() {
        vectorStore = new FakeRagVectorStore();
        tagRepository = new FakeRagTagRepository();
        service = new RagService(vectorStore, new TokenTextSplitter(), tagRepository, null);
    }

    @Test
    void uploadFilesAddsDocumentsWithKnowledgeAndUserMetadata() {
        service.uploadFiles(7L, "spring-ai", List.of(new RagFile("note.md", "Spring AI supports vector search.")));

        assertThat(vectorStore.addedDocuments).isNotEmpty();
        assertThat(vectorStore.addedDocuments)
                .anySatisfy(document -> {
                    assertThat(document.getText()).contains("Spring AI supports vector search.");
                    assertThat(document.getMetadata()).containsEntry("knowledge", "spring-ai");
                    assertThat(document.getMetadata()).containsEntry("userId", "7");
                    assertThat(document.getMetadata()).containsEntry("source", "note.md");
                });
    }

    @Test
    void listTagsReturnsUserKnowledgeTags() {
        tagRepository.tags = List.of("spring-ai", "pgvector");

        List<RagTagVO> tags = service.listTags(7L);

        assertThat(tags).extracting(RagTagVO::ragTag).containsExactly("spring-ai", "pgvector");
    }

    @Test
    void retrieveUsesPgVectorStoreSimilaritySearchWithKnowledgeAndUserFilter() {
        Document result = new Document("pgvector stores vectors");
        result.getMetadata().put("source", "note.md");
        vectorStore.searchResults = List.of(result);

        List<RagChunk> chunks = service.retrieve(7L, "spring-ai", "How to store vectors?");

        assertThat(vectorStore.searchRequest.getTopK()).isEqualTo(3);
        assertThat(vectorStore.searchRequest.getQuery()).isEqualTo("How to store vectors?");
        assertThat(vectorStore.searchRequest.getFilterExpression().toString())
                .contains("knowledge", "spring-ai", "userId", "7");
        assertThat(chunks).extracting(RagChunk::content).containsExactly("pgvector stores vectors");
        assertThat(chunks).extracting(RagChunk::source).containsExactly("note.md");
    }

    @Test
    void uploadRejectsBlankTag() {
        assertThatThrownBy(() -> service.uploadFiles(7L, " ", List.of(new RagFile("note.md", "text"))))
                .isInstanceOf(RagException.class)
                .hasMessage("知识库标签不能为空");
    }

    private static class FakeRagVectorStore implements IRagVectorStore {
        private List<Document> addedDocuments = new ArrayList<>();
        private SearchRequest searchRequest;
        private List<Document> searchResults = new ArrayList<>();

        @Override
        public void add(List<Document> documents) {
            addedDocuments = documents;
        }

        @Override
        public List<Document> similaritySearch(SearchRequest request) {
            searchRequest = request;
            return searchResults;
        }
    }

    private static class FakeRagTagRepository implements IRagTagRepository {
        private List<String> tags = new ArrayList<>();

        @Override
        public List<String> listTags(Long userId) {
            return tags;
        }
    }
}
