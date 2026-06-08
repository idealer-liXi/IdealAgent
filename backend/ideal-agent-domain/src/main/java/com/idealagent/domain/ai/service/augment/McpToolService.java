package com.idealagent.domain.ai.service.augment;

import com.idealagent.domain.ai.model.entity.AiConfigRecord;
import com.idealagent.domain.ai.model.enumeration.ConfigKind;
import com.idealagent.domain.ai.repository.IAiConfigRepository;
import com.idealagent.domain.ai.service.armory.mcp.IMcpClientArmory;
import com.idealagent.domain.ai.service.mcp.McpException;
import io.modelcontextprotocol.client.McpSyncClient;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Service
public class McpToolService implements IMcpToolService {
    private static final Logger log = LoggerFactory.getLogger(McpToolService.class);
    private static final int ENABLED = 1;

    private final IAiConfigRepository aiConfigRepository;
    private final IMcpClientArmory mcpClientArmory;

    public McpToolService(IAiConfigRepository aiConfigRepository, IMcpClientArmory mcpClientArmory) {
        this.aiConfigRepository = aiConfigRepository;
        this.mcpClientArmory = mcpClientArmory;
    }

    @Override
    public McpToolHandle augmentMcpTool(Long userId, String clientId) {
        if (!StringUtils.hasText(clientId)) {
            return McpToolHandle.empty();
        }
        List<AiConfigRecord> bindings = aiConfigRepository.list(ConfigKind.CONFIG).stream()
                .filter(this::enabled)
                .filter(record -> "client".equals(record.getOwnerType()))
                .filter(record -> clientId.equals(record.getContent()))
                .filter(record -> "mcp".equals(record.getConfigType()))
                .filter(record -> StringUtils.hasText(record.getRefId()))
                .toList();
        if (bindings.isEmpty()) {
            return McpToolHandle.empty();
        }

        return buildTools(userId, bindings.stream().map(AiConfigRecord::getRefId).toList(), clientId);
    }

    @Override
    public McpToolHandle augmentMcpTool(Long userId, List<String> mcpIds) {
        if (mcpIds == null || mcpIds.isEmpty()) {
            return McpToolHandle.empty();
        }
        return buildTools(userId, mcpIds.stream().filter(StringUtils::hasText).distinct().toList(), null);
    }

    private McpToolHandle buildTools(Long userId, List<String> mcpIds, String clientId) {
        if (mcpIds == null || mcpIds.isEmpty()) {
            return McpToolHandle.empty();
        }
        List<McpSyncClient> clients = new ArrayList<>();
        for (String mcpId : mcpIds) {
            AiConfigRecord mcpRecord = aiConfigRepository.find(ConfigKind.MCP, mcpId);
            if (mcpRecord == null) {
                closeClients(clients);
                throw new McpException("MCP 配置不存在");
            }
            if (!enabled(mcpRecord)) {
                continue;
            }
            try {
                clients.add(mcpClientArmory.build(mcpRecord, userId));
            } catch (RuntimeException e) {
                log.warn("MCP 初始化失败，已跳过：clientId={}, mcpId={}, reason={}", clientId, mcpRecord.getConfigId(), e.getMessage());
            }
        }
        if (clients.isEmpty()) {
            return McpToolHandle.empty();
        }
        return new McpToolHandle(new SyncMcpToolCallbackProvider(clients), clients);
    }

    private boolean enabled(AiConfigRecord record) {
        return record.getStatus() != null && record.getStatus() == ENABLED;
    }

    private void closeClients(List<McpSyncClient> clients) {
        for (McpSyncClient client : clients) {
            client.close();
        }
    }
}
