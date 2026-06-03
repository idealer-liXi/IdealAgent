package com.idealagent.domain.ai.model.entity;

public record RagChunk(String content, String source, float[] embedding) {
}
