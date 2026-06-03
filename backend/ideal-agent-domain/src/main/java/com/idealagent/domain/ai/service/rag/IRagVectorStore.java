package com.idealagent.domain.ai.service.rag;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;

import java.util.List;

public interface IRagVectorStore {
    void add(List<Document> documents);

    List<Document> similaritySearch(SearchRequest request);
}
