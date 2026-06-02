package com.idealagent.domain.rag.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class DeterministicEmbeddingService implements IEmbeddingService {
    public static final int DIMENSION = 1024;

    @Override
    public float[] embed(String text) {
        float[] vector = new float[DIMENSION];
        byte[] bytes = text == null ? new byte[0] : text.getBytes(StandardCharsets.UTF_8);
        for (int i = 0; i < DIMENSION; i++) {
            byte[] digest = digest(bytes, i);
            int value = ((digest[0] & 0xff) << 8) | (digest[1] & 0xff);
            vector[i] = (value / 32767.5f) - 1.0f;
        }
        return vector;
    }

    private byte[] digest(byte[] bytes, int salt) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update((byte) (salt & 0xff));
            digest.update((byte) ((salt >> 8) & 0xff));
            digest.update(bytes);
            return digest.digest();
        } catch (NoSuchAlgorithmException e) {
            throw new RagException("生成向量失败", e);
        }
    }
}
