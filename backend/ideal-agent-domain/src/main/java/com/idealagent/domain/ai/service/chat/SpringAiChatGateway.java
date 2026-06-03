package com.idealagent.domain.ai.service.chat;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.function.Consumer;

@Service
public class SpringAiChatGateway {
    public String complete(ChatClient chatClient, List<Message> messages) {
        return complete(chatClient, messages, null);
    }

    public String complete(ChatClient chatClient, List<Message> messages, ToolCallbackProvider toolCallbackProvider) {
        ChatClient.ChatClientRequestSpec request = chatClient.prompt().messages(messages);
        if (toolCallbackProvider != null) {
            request = request.toolCallbacks(toolCallbackProvider);
        }
        String content = request.call().content();
        if (content == null || content.isBlank()) {
            throw new ChatException("模型返回内容为空");
        }
        return content;
    }

    public void stream(ChatClient chatClient, List<Message> messages, Consumer<String> onDelta) {
        stream(chatClient, messages, null, onDelta);
    }

    public void stream(ChatClient chatClient, List<Message> messages, ToolCallbackProvider toolCallbackProvider, Consumer<String> onDelta) {
        ChatClient.ChatClientRequestSpec request = chatClient.prompt().messages(messages);
        if (toolCallbackProvider != null) {
            request = request.toolCallbacks(toolCallbackProvider);
        }
        request.stream().content().toIterable().forEach(onDelta);
    }
}
