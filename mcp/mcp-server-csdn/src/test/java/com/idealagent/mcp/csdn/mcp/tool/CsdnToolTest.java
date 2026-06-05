package com.idealagent.mcp.csdn.mcp.tool;

import com.idealagent.mcp.csdn.mcp.dto.SaveArticleToolRequest;
import com.idealagent.mcp.csdn.mcp.dto.SaveArticleToolResponse;
import com.idealagent.mcp.csdn.mcp.port.ICsdnPort;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class CsdnToolTest {

    @Test
    void saveArticleDelegatesRequestToPort() throws IOException {
        CsdnTool tool = new CsdnTool();
        FakeCsdnPort port = new FakeCsdnPort();
        ReflectionTestUtils.setField(tool, "postCsdnPort", port);

        SaveArticleToolRequest request = new SaveArticleToolRequest();
        request.setTitle("Stage 6 MCP Migration");
        request.setMarkdownContent("# IdealAgent");

        SaveArticleToolResponse response = tool.saveArticle(request);

        assertThat(port.receivedRequest).isSameAs(request);
        assertThat(response).isSameAs(port.response);
    }

    private static class FakeCsdnPort implements ICsdnPort {
        private final SaveArticleToolResponse response = new SaveArticleToolResponse();
        private SaveArticleToolRequest receivedRequest;

        @Override
        public SaveArticleToolResponse saveArticle(SaveArticleToolRequest request) {
            receivedRequest = request;
            return response;
        }
    }
}
