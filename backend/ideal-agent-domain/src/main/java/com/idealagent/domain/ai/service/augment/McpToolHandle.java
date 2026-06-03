package com.idealagent.domain.ai.service.augment;

import io.modelcontextprotocol.client.McpSyncClient;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;

import java.util.List;

public class McpToolHandle implements AutoCloseable {
    private static final McpToolHandle EMPTY = new McpToolHandle(new SyncMcpToolCallbackProvider(), List.of());

    private final SyncMcpToolCallbackProvider toolCallbackProvider;
    private final List<McpSyncClient> clients;

    public McpToolHandle(SyncMcpToolCallbackProvider toolCallbackProvider, List<McpSyncClient> clients) {
        this.toolCallbackProvider = toolCallbackProvider;
        this.clients = clients;
    }

    public static McpToolHandle empty() {
        return EMPTY;
    }

    public SyncMcpToolCallbackProvider toolCallbackProvider() {
        return toolCallbackProvider;
    }

    @Override
    public void close() {
        for (McpSyncClient client : clients) {
            client.close();
        }
    }
}
