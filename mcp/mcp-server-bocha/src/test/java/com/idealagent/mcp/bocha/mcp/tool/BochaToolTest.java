package com.idealagent.mcp.bocha.mcp.tool;

import com.idealagent.mcp.bocha.mcp.dto.BochaSearchToolRequest;
import com.idealagent.mcp.bocha.mcp.dto.BochaSearchToolResponse;
import com.idealagent.mcp.bocha.mcp.port.IBochaPort;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class BochaToolTest {

    @Test
    void webSearchDelegatesRequestToPort() throws IOException {
        BochaTool tool = new BochaTool();
        FakeBochaPort port = new FakeBochaPort();
        ReflectionTestUtils.setField(tool, "bochaPort", port);

        BochaSearchToolRequest request = new BochaSearchToolRequest();
        request.setQuery("Spring AI MCP server 1.0.0");
        request.setFreshness("oneWeek");

        BochaSearchToolResponse response = tool.webSearch(request);

        assertThat(port.receivedRequest).isSameAs(request);
        assertThat(response).isSameAs(port.response);
    }

    private static class FakeBochaPort implements IBochaPort {
        private final BochaSearchToolResponse response = new BochaSearchToolResponse();
        private BochaSearchToolRequest receivedRequest;

        @Override
        public BochaSearchToolResponse webSearch(BochaSearchToolRequest toolRequest) {
            receivedRequest = toolRequest;
            return response;
        }
    }
}
