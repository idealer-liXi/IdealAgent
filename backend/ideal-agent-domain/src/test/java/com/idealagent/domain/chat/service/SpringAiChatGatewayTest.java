package com.idealagent.domain.ai.service.chat;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SpringAiChatGatewayTest {
    @Test
    void completeAttachesToolCallbackProvider() {
        ChatClient chatClient = mock(ChatClient.class);
        ChatClient.ChatClientRequestSpec requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        ChatClient.CallResponseSpec responseSpec = mock(ChatClient.CallResponseSpec.class);
        SyncMcpToolCallbackProvider provider = new SyncMcpToolCallbackProvider();
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.messages(any(List.class))).thenReturn(requestSpec);
        when(requestSpec.toolCallbacks(provider)).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(responseSpec);
        when(responseSpec.content()).thenReturn("ok");

        new SpringAiChatGateway().complete(chatClient, List.of(new UserMessage("hi")), provider);

        verify(requestSpec).toolCallbacks(provider);
    }
}
