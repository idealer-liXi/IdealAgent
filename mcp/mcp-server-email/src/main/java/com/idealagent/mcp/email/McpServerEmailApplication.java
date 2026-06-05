package com.idealagent.mcp.email;

import com.idealagent.mcp.email.mcp.tool.EmailTool;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class McpServerEmailApplication {

    public static void main(String[] args) {
        SpringApplication.run(McpServerEmailApplication.class, args);
    }

    @Bean
    public ToolCallbackProvider createTool(EmailTool emailTool) {
        return MethodToolCallbackProvider.builder().toolObjects(emailTool).build();
    }
}
