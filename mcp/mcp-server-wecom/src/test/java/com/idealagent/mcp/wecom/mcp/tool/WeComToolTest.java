package com.idealagent.mcp.wecom.mcp.tool;

import com.idealagent.mcp.wecom.mcp.dto.SendMessageToolResponse;
import com.idealagent.mcp.wecom.mcp.dto.SendTextCardToolRequest;
import com.idealagent.mcp.wecom.mcp.dto.SendTextToolRequest;
import com.idealagent.mcp.wecom.mcp.port.IWeComPort;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class WeComToolTest {

    @Test
    void sendTextCardDelegatesRequestToPort() throws IOException {
        WeComTool tool = new WeComTool();
        FakeWeComPort port = new FakeWeComPort();
        ReflectionTestUtils.setField(tool, "weComPort", port);

        SendTextCardToolRequest request = new SendTextCardToolRequest();
        request.setTitle("Stage 6");
        request.setDescription("WeCom migration");
        request.setUrl("https://example.com");

        SendMessageToolResponse response = tool.sendTextCard(request);

        assertThat(port.receivedTextCardRequest).isSameAs(request);
        assertThat(response).isSameAs(port.textCardResponse);
    }

    @Test
    void sendTextDelegatesRequestToPort() throws IOException {
        WeComTool tool = new WeComTool();
        FakeWeComPort port = new FakeWeComPort();
        ReflectionTestUtils.setField(tool, "weComPort", port);

        SendTextToolRequest request = new SendTextToolRequest();
        request.setContent("IdealAgent WeCom MCP");

        SendMessageToolResponse response = tool.sendText(request);

        assertThat(port.receivedTextRequest).isSameAs(request);
        assertThat(response).isSameAs(port.textResponse);
    }

    private static class FakeWeComPort implements IWeComPort {
        private final SendMessageToolResponse textCardResponse = new SendMessageToolResponse();
        private final SendMessageToolResponse textResponse = new SendMessageToolResponse();
        private SendTextCardToolRequest receivedTextCardRequest;
        private SendTextToolRequest receivedTextRequest;

        @Override
        public SendMessageToolResponse sendTextCard(SendTextCardToolRequest toolRequest) {
            receivedTextCardRequest = toolRequest;
            return textCardResponse;
        }

        @Override
        public SendMessageToolResponse sendText(SendTextToolRequest toolRequest) {
            receivedTextRequest = toolRequest;
            return textResponse;
        }
    }
}
