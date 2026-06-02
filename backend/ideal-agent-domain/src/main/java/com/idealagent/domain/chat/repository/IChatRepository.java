package com.idealagent.domain.chat.repository;

import com.idealagent.domain.chat.model.entity.ChatMessage;
import com.idealagent.domain.chat.model.entity.ChatSession;

import java.util.List;
import java.util.Optional;

public interface IChatRepository {
    Optional<ChatSession> findSession(String sessionId, Long userId);

    ChatSession saveSession(ChatSession session);

    List<ChatSession> listSessions(Long userId);

    ChatMessage saveMessage(ChatMessage message);

    List<ChatMessage> listMessages(String sessionId, Long userId);
}
