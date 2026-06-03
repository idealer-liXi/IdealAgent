package com.idealagent.domain.session.repository;

import com.idealagent.domain.session.model.entity.ChatMessage;
import com.idealagent.domain.session.model.entity.ChatSession;

import java.util.List;
import java.util.Optional;

public interface ISessionRepository {
    Optional<ChatSession> findSession(String sessionId, Long userId);

    ChatSession saveSession(ChatSession session);

    List<ChatSession> listSessions(Long userId);

    ChatMessage saveMessage(ChatMessage message);

    List<ChatMessage> listMessages(String sessionId, Long userId);
}
