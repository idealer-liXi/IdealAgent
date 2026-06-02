package com.idealagent.domain.rag.model.entity;

public record RagChunk(String content, String source, float[] embedding) {
}
