package com.idealagent.domain.ai.model.enumeration;

import com.idealagent.domain.ai.service.mcp.McpException;

import java.util.Locale;

public enum McpTransportType {
    STDIO("stdio"),
    SSE("sse");

    private final String value;

    McpTransportType(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public static McpTransportType from(String value) {
        if (value == null) {
            throw new McpException("MCP 类型不支持");
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        for (McpTransportType type : values()) {
            if (type.value.equals(normalized)) {
                return type;
            }
        }
        throw new McpException("MCP 类型不支持");
    }
}
