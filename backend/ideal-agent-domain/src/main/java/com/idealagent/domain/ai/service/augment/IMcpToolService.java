package com.idealagent.domain.ai.service.augment;

import java.util.List;

public interface IMcpToolService {
    McpToolHandle augmentMcpTool(Long userId, String clientId);

    default McpToolHandle augmentMcpTool(Long userId, List<String> mcpIds) {
        return McpToolHandle.empty();
    }
}
