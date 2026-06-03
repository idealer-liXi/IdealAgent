package com.idealagent.domain.ai.model.entity;

import com.idealagent.domain.ai.service.mcp.McpException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class McpServerConfigTest {
    @Test
    void parseStdioConfig() {
        McpServerConfig config = McpServerConfig.parse("""
                {"command":"node","args":["server.js"],"env":{"KEY":"value"},"timeoutMinutes":5}
                """);

        assertThat(config.command()).isEqualTo("node");
        assertThat(config.args()).containsExactly("server.js");
        assertThat(config.env()).containsEntry("KEY", "value");
        assertThat(config.timeoutMinutes()).isEqualTo(5);
    }

    @Test
    void parseSseConfig() {
        McpServerConfig config = McpServerConfig.parse("""
                {"baseUri":"http://localhost:18080","sseEndpoint":"/sse","timeoutMinutes":2}
                """);

        assertThat(config.baseUri()).isEqualTo("http://localhost:18080");
        assertThat(config.sseEndpoint()).isEqualTo("/sse");
        assertThat(config.timeoutMinutes()).isEqualTo(2);
    }

    @Test
    void parseDefaultsCollectionsAndTimeout() {
        McpServerConfig config = McpServerConfig.parse("{} ");

        assertThat(config.args()).isEqualTo(List.of());
        assertThat(config.env()).isEqualTo(Map.of());
        assertThat(config.timeoutMinutes()).isEqualTo(3);
    }

    @Test
    void parseRejectsInvalidJson() {
        assertThatThrownBy(() -> McpServerConfig.parse("{"))
                .isInstanceOf(McpException.class)
                .hasMessage("MCP 配置内容不是合法 JSON");
    }
}
