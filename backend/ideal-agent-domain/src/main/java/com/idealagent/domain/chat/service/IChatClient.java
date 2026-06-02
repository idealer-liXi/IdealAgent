package com.idealagent.domain.chat.service;

import com.idealagent.domain.chat.model.entity.ChatMessage;

import java.util.List;

public interface IChatClient {
    String complete(String clientId, List<ChatMessage> history);
}
