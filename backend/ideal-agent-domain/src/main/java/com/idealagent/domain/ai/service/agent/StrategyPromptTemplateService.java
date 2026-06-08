package com.idealagent.domain.ai.service.agent;

import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Service
public class StrategyPromptTemplateService {
    private final ResourceLoader resourceLoader;

    public StrategyPromptTemplateService(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    public String systemPrompt(String strategy, String role) {
        return load(strategy, "system-prompt", role);
    }

    public String userPrompt(String strategy, String role) {
        return load(strategy, "user-prompt", role);
    }

    private String load(String strategy, String promptKind, String role) {
        String path = "classpath:template/" + normalize(strategy) + "/" + promptKind + "/" + roleFileName(role) + ".md";
        Resource resource = resourceLoader.getResource(path);
        if (!resource.exists()) {
            return "";
        }
        try (InputStream inputStream = resource.getInputStream()) {
            return StreamUtils.copyToString(inputStream, StandardCharsets.UTF_8).trim();
        } catch (IOException e) {
            return "";
        }
    }

    private String roleFileName(String role) {
        String normalized = normalize(role);
        if (!StringUtils.hasText(normalized)) {
            return "";
        }
        return Character.toUpperCase(normalized.charAt(0)) + normalized.substring(1);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }
}
