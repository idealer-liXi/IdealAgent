package com.idealagent.domain.chat.service;

import com.idealagent.domain.chat.model.entity.ChatMessage;

import java.util.List;
import java.util.function.Consumer;

public interface IChatClient {
    String complete(String clientId, List<ChatMessage> history);

    default void stream(String clientId, List<ChatMessage> history, Consumer<String> onDelta) {
        onDelta.accept(complete(clientId, history));
    }
}
