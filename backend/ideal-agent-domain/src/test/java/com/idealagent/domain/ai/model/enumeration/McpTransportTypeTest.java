package com.idealagent.domain.ai.model.enumeration;

import com.idealagent.domain.ai.service.mcp.McpException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class McpTransportTypeTest {
    @Test
    void fromAcceptsSupportedTransportTypes() {
        assertThat(McpTransportType.from("stdio")).isEqualTo(McpTransportType.STDIO);
        assertThat(McpTransportType.from("sse")).isEqualTo(McpTransportType.SSE);
        assertThat(McpTransportType.from(" STDIO ")).isEqualTo(McpTransportType.STDIO);
    }

    @Test
    void fromRejectsUnsupportedTransportType() {
        assertThatThrownBy(() -> McpTransportType.from("websocket"))
                .isInstanceOf(McpException.class)
                .hasMessage("MCP 类型不支持");
    }
}
