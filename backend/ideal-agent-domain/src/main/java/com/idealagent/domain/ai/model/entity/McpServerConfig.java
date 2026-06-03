package com.idealagent.domain.ai.model.entity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.idealagent.domain.ai.service.mcp.McpException;

import java.util.List;
import java.util.Map;

public record McpServerConfig(
        String command,
        List<String> args,
        Map<String, String> env,
        String baseUri,
        String sseEndpoint,
        long timeoutMinutes) {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final long DEFAULT_TIMEOUT_MINUTES = 3L;

    public static McpServerConfig parse(String content) {
        try {
            Map<String, Object> map = OBJECT_MAPPER.readValue(content == null || content.isBlank() ? "{}" : content, new TypeReference<>() {
            });
            return new McpServerConfig(
                    stringValue(map.get("command")),
                    stringList(map.get("args")),
                    stringMap(map.get("env")),
                    stringValue(map.get("baseUri")),
                    stringValue(map.get("sseEndpoint")),
                    timeout(map.get("timeoutMinutes")));
        } catch (JsonProcessingException e) {
            throw new McpException("MCP 配置内容不是合法 JSON", e);
        }
    }

    private static String stringValue(Object value) {
        return value == null ? null : value.toString();
    }

    private static List<String> stringList(Object value) {
        if (!(value instanceof List<?> values)) {
            return List.of();
        }
        return values.stream().map(Object::toString).toList();
    }

    private static Map<String, String> stringMap(Object value) {
        if (!(value instanceof Map<?, ?> values)) {
            return Map.of();
        }
        return values.entrySet().stream().collect(java.util.stream.Collectors.toMap(
                entry -> entry.getKey().toString(),
                entry -> entry.getValue() == null ? "" : entry.getValue().toString()));
    }

    private static long timeout(Object value) {
        if (value instanceof Number number && number.longValue() > 0) {
            return number.longValue();
        }
        if (value instanceof String text) {
            try {
                long parsed = Long.parseLong(text);
                return parsed > 0 ? parsed : DEFAULT_TIMEOUT_MINUTES;
            } catch (NumberFormatException ignored) {
                return DEFAULT_TIMEOUT_MINUTES;
            }
        }
        return DEFAULT_TIMEOUT_MINUTES;
    }
}
