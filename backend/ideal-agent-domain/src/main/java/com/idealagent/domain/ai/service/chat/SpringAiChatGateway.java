package com.idealagent.domain.ai.service.chat;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.function.Consumer;

@Service
public class SpringAiChatGateway {
    public String complete(ChatClient chatClient, List<Message> messages) {
        String content = chatClient.prompt().messages(messages).call().content();
        if (content == null || content.isBlank()) {
            throw new ChatException("模型返回内容为空");
        }
        return content;
    }

    public void stream(ChatClient chatClient, List<Message> messages, Consumer<String> onDelta) {
        chatClient.prompt().messages(messages).stream().content().toIterable().forEach(onDelta);
    }
}
