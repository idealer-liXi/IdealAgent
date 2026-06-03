package com.idealagent.domain.ai.service.armory.mcp;

import com.idealagent.domain.ai.model.entity.AiConfigRecord;
import io.modelcontextprotocol.client.McpSyncClient;

public interface IMcpClientArmory {
    McpSyncClient build(AiConfigRecord mcpRecord, Long userId);
}
