package com.idealagent.domain.ai.service.armory.mcp;

import com.idealagent.domain.ai.model.entity.AiConfigRecord;
import com.idealagent.domain.ai.service.mcp.McpException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class McpClientArmoryTest {
    private final McpClientArmory armory = new McpClientArmory("X-IdealAgent-Mcp-Secret", "X-IdealAgent-Mcp-UserId");

    @Test
    void buildRejectsBlankStdioCommand() {
        AiConfigRecord record = record("stdio", "{}");

        assertThatThrownBy(() -> armory.build(record, 7L))
                .isInstanceOf(McpException.class)
                .hasMessage("MCP stdio command 不能为空");
    }

    @Test
    void buildRejectsBlankSseBaseUri() {
        AiConfigRecord record = record("sse", "{\"sseEndpoint\":\"/sse\"}");

        assertThatThrownBy(() -> armory.build(record, 7L))
                .isInstanceOf(McpException.class)
                .hasMessage("MCP SSE baseUri 不能为空");
    }

    private AiConfigRecord record(String type, String content) {
        AiConfigRecord record = new AiConfigRecord();
        record.setConfigId("mcp_test");
        record.setType(type);
        record.setContent(content);
        record.setStatus(1);
        return record;
    }
}
