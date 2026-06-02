package com.idealagent.domain.rag.repository;

import com.idealagent.domain.rag.model.entity.RagChunk;

import java.util.List;

public interface IRagRepository {
    void saveChunks(Long userId, String ragTag, List<RagChunk> chunks);

    List<String> listTags(Long userId);

    List<RagChunk> search(Long userId, String ragTag, float[] queryEmbedding, int limit);
}
