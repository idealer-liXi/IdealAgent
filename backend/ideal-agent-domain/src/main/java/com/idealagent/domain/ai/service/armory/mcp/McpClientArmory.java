package com.idealagent.domain.ai.service.armory.mcp;

import com.idealagent.domain.ai.model.entity.AiConfigRecord;
import com.idealagent.domain.ai.model.entity.McpServerConfig;
import com.idealagent.domain.ai.model.enumeration.McpTransportType;
import com.idealagent.domain.ai.service.mcp.McpException;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

@Service
public class McpClientArmory implements IMcpClientArmory {
    private final String mcpSecretHeader;
    private final String mcpUserIdHeader;

    public McpClientArmory(
            @Value("${ideal-agent.mcp.header.secret:X-IdealAgent-Mcp-Secret}") String mcpSecretHeader,
            @Value("${ideal-agent.mcp.header.user-id:X-IdealAgent-Mcp-UserId}") String mcpUserIdHeader) {
        this.mcpSecretHeader = mcpSecretHeader;
        this.mcpUserIdHeader = mcpUserIdHeader;
    }

    @Override
    public McpSyncClient build(AiConfigRecord mcpRecord, Long userId) {
        McpServerConfig config = McpServerConfig.parse(mcpRecord.getContent());
        try {
            McpSyncClient client = switch (McpTransportType.from(mcpRecord.getType())) {
                case STDIO -> stdioClient(config);
                case SSE -> sseClient(config, mcpRecord.getSecret(), userId);
            };
            client.initialize();
            return client;
        } catch (McpException e) {
            throw e;
        } catch (Exception e) {
            throw new McpException("MCP 工具初始化失败", e);
        }
    }

    private McpSyncClient stdioClient(McpServerConfig config) {
        if (!StringUtils.hasText(config.command())) {
            throw new McpException("MCP stdio command 不能为空");
        }
        ServerParameters serverParameters = ServerParameters.builder(config.command())
                .args(config.args())
                .env(config.env())
                .build();
        return McpClient.sync(new StdioClientTransport(serverParameters))
                .requestTimeout(Duration.ofMinutes(config.timeoutMinutes()))
                .build();
    }

    private McpSyncClient sseClient(McpServerConfig config, String secret, Long userId) {
        if (!StringUtils.hasText(config.baseUri())) {
            throw new McpException("MCP SSE baseUri 不能为空");
        }
        HttpClientSseClientTransport transport = HttpClientSseClientTransport.builder(config.baseUri())
                .sseEndpoint(StringUtils.hasText(config.sseEndpoint()) ? config.sseEndpoint() : "/sse")
                .customizeRequest(requestBuilder -> {
                    requestBuilder.header(mcpUserIdHeader, String.valueOf(userId));
                    if (StringUtils.hasText(secret)) {
                        requestBuilder.header(mcpSecretHeader, Base64.getEncoder().encodeToString(secret.getBytes(StandardCharsets.UTF_8)));
                    }
                })
                .build();
        return McpClient.sync(transport)
                .requestTimeout(Duration.ofMinutes(config.timeoutMinutes()))
                .build();
    }
}
