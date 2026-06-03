package com.idealagent.infrastructure.rag;

import com.idealagent.domain.ai.service.rag.IRagVectorStore;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;

import java.util.List;

public class PgVectorStoreAdapter implements IRagVectorStore {
    private final PgVectorStore pgVectorStore;

    public PgVectorStoreAdapter(PgVectorStore pgVectorStore) {
        this.pgVectorStore = pgVectorStore;
    }

    @Override
    public void add(List<Document> documents) {
        pgVectorStore.add(documents);
    }

    @Override
    public List<Document> similaritySearch(SearchRequest request) {
        return pgVectorStore.similaritySearch(request);
    }
}
