package com.idealagent.mcp.email.mcp.tool;

import com.idealagent.mcp.email.mcp.dto.SendEmailToolRequest;
import com.idealagent.mcp.email.mcp.dto.SendEmailToolResponse;
import com.idealagent.mcp.email.mcp.port.IEmailPort;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class EmailToolTest {

    @Test
    void sendEmailDelegatesRequestToPort() throws IOException {
        EmailTool tool = new EmailTool();
        FakeEmailPort port = new FakeEmailPort();
        ReflectionTestUtils.setField(tool, "emailPort", port);

        SendEmailToolRequest request = new SendEmailToolRequest();
        request.setTo("receiver@example.com");
        request.setSubject("Stage 6 MCP Migration");
        request.setContent("IdealAgent Email MCP");

        SendEmailToolResponse response = tool.sendEmail(request);

        assertThat(port.receivedRequest).isSameAs(request);
        assertThat(response).isSameAs(port.response);
    }

    private static class FakeEmailPort implements IEmailPort {
        private final SendEmailToolResponse response = new SendEmailToolResponse();
        private SendEmailToolRequest receivedRequest;

        @Override
        public SendEmailToolResponse sendEmail(SendEmailToolRequest toolRequest) {
            receivedRequest = toolRequest;
            return response;
        }
    }
}
