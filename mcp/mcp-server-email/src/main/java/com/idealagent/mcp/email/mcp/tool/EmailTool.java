package com.idealagent.mcp.email.mcp.tool;

import com.idealagent.mcp.email.mcp.dto.SendEmailToolRequest;
import com.idealagent.mcp.email.mcp.dto.SendEmailToolResponse;
import com.idealagent.mcp.email.mcp.port.IEmailPort;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Slf4j
@Service
public class EmailTool {

    @Resource
    private IEmailPort emailPort;

    @Tool(description = "发送电子邮件")
    public SendEmailToolResponse sendEmail(SendEmailToolRequest toolRequest) throws IOException {
        log.info("调用 MCP 工具发送邮件：to={} subject={}", toolRequest.getTo(), toolRequest.getSubject());
        return emailPort.sendEmail(toolRequest);
    }
}
