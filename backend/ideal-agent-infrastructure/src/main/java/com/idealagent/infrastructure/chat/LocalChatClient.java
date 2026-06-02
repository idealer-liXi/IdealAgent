package com.idealagent.infrastructure.chat;

import com.idealagent.domain.chat.model.entity.ChatMessage;
import com.idealagent.domain.chat.service.IChatClient;

import java.util.List;

public class LocalChatClient implements IChatClient {
    @Override
    public String complete(String clientId, List<ChatMessage> history) {
        ChatMessage latest = history.get(history.size() - 1);
        return "IdealAgent local chat client [" + clientId + "] received: " + latest.getContent();
    }
}
