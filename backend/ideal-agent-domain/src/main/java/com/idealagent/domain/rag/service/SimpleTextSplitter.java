package com.idealagent.domain.rag.service;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Service
public class SimpleTextSplitter implements ITextSplitter {
    private static final int CHUNK_SIZE = 500;
    private static final int OVERLAP = 80;

    @Override
    public List<String> split(String text) {
        if (!StringUtils.hasText(text)) {
            return List.of();
        }
        String normalized = text.replace("\r\n", "\n").trim();
        List<String> chunks = new ArrayList<>();
        int start = 0;
        while (start < normalized.length()) {
            int end = Math.min(start + CHUNK_SIZE, normalized.length());
            chunks.add(normalized.substring(start, end));
            if (end == normalized.length()) {
                break;
            }
            start = Math.max(0, end - OVERLAP);
        }
        return chunks;
    }
}
