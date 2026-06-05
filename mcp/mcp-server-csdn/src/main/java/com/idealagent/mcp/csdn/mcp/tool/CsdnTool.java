package com.idealagent.mcp.csdn.mcp.tool;

import com.idealagent.mcp.csdn.mcp.dto.SaveArticleToolRequest;
import com.idealagent.mcp.csdn.mcp.dto.SaveArticleToolResponse;
import com.idealagent.mcp.csdn.mcp.port.ICsdnPort;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
public class CsdnTool {

    @Resource
    private ICsdnPort postCsdnPort;

    @Tool(description = "发布文章到 CSDN")
    public SaveArticleToolResponse saveArticle(SaveArticleToolRequest toolRequest) throws IOException {
        log.info("调用 MCP 工具进行 CSDN 发帖：标题={}", toolRequest.getTitle());
        return postCsdnPort.saveArticle(toolRequest);
    }
}
