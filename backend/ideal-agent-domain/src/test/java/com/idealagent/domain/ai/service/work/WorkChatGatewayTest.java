package com.idealagent.domain.ai.service.work;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.tool.ToolCallbackProvider;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WorkChatGatewayTest {
    private final WorkChatGateway gateway = new WorkChatGateway();

    @Test
    void passesToolCallbacksWhenProviderSupplied() {
        ChatClient chatClient = mock(ChatClient.class);
        ChatClient.ChatClientRequestSpec request = mock(ChatClient.ChatClientRequestSpec.class);
        ChatClient.CallResponseSpec response = mock(ChatClient.CallResponseSpec.class);
        ToolCallbackProvider provider = mock(ToolCallbackProvider.class);
        when(chatClient.prompt("run task")).thenReturn(request);
        when(request.toolCallbacks(provider)).thenReturn(request);
        when(request.call()).thenReturn(response);
        when(response.content()).thenReturn("done");

        String result = gateway.complete(chatClient, "run task", provider);

        assertThat(result).isEqualTo("done");
        verify(request).toolCallbacks(provider);
    }

    @Test
    void sendsBuiltMessagesWhenProvided() {
        ChatClient chatClient = mock(ChatClient.class);
        ChatClient.ChatClientRequestSpec request = mock(ChatClient.ChatClientRequestSpec.class);
        ChatClient.CallResponseSpec response = mock(ChatClient.CallResponseSpec.class);
        ToolCallbackProvider provider = mock(ToolCallbackProvider.class);
        List<Message> messages = List.of(new UserMessage("run task"));
        when(chatClient.prompt()).thenReturn(request);
        when(request.messages(messages)).thenReturn(request);
        when(request.toolCallbacks(provider)).thenReturn(request);
        when(request.call()).thenReturn(response);
        when(response.content()).thenReturn("done");

        String result = gateway.complete(chatClient, messages, provider);

        assertThat(result).isEqualTo("done");
        verify(request).messages(messages);
        verify(request).toolCallbacks(provider);
    }

    @Test
    void rejectsBlankModelContent() {
        ChatClient chatClient = mock(ChatClient.class);
        ChatClient.ChatClientRequestSpec request = mock(ChatClient.ChatClientRequestSpec.class);
        ChatClient.CallResponseSpec response = mock(ChatClient.CallResponseSpec.class);
        when(chatClient.prompt("run task")).thenReturn(request);
        when(request.call()).thenReturn(response);
        when(response.content()).thenReturn(" ");

        assertThatThrownBy(() -> gateway.complete(chatClient, "run task"))
                .isInstanceOf(WorkException.class)
                .hasMessage("模型返回内容为空");
    }
}
