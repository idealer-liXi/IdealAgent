package com.idealagent.infrastructure.repository;

import com.idealagent.domain.chat.model.entity.ChatMessage;
import com.idealagent.domain.chat.model.entity.ChatSession;
import com.idealagent.infrastructure.persistent.dao.IAiMessageDao;
import com.idealagent.infrastructure.persistent.dao.IAiSessionDao;
import com.idealagent.infrastructure.persistent.po.AiMessage;
import com.idealagent.infrastructure.persistent.po.AiSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ChatRepositoryTest {
    private ChatRepository repository;

    @BeforeEach
    void setUp() {
        repository = new ChatRepository(new FakeSessionDao(), new FakeMessageDao());
    }

    @Test
    void savesSessionAndMessageThenListsByUserSession() {
        ChatSession session = new ChatSession();
        session.setSessionId("session_1");
        session.setType("chat");
        session.setTitle("hello");
        session.setUserId(7L);
        session.setTargetId("client_default_chat");
        repository.saveSession(session);

        ChatMessage message = new ChatMessage();
        message.setMessageId("msg_1");
        message.setSessionId("session_1");
        message.setType("chat");
        message.setRole("user");
        message.setContent("hello");
        repository.saveMessage(message);

        assertThat(repository.findSession("session_1", 7L)).isPresent();
        assertThat(repository.listSessions(7L)).extracting(ChatSession::getSessionId).containsExactly("session_1");
        assertThat(repository.listMessages("session_1", 7L)).extracting(ChatMessage::getContent).containsExactly("hello");
    }

    private static class FakeSessionDao implements IAiSessionDao {
        private final List<AiSession> sessions = new ArrayList<>();

        @Override
        public int insert(AiSession session) {
            sessions.add(session);
            return 1;
        }

        @Override
        public AiSession queryBySessionIdAndUserId(String sessionId, Long userId) {
            return sessions.stream()
                    .filter(session -> session.getSessionId().equals(sessionId) && session.getUserId().equals(userId))
                    .findFirst()
                    .orElse(null);
        }

        @Override
        public List<AiSession> listByUserId(Long userId) {
            return sessions.stream().filter(session -> session.getUserId().equals(userId)).toList();
        }
    }

    private static class FakeMessageDao implements IAiMessageDao {
        private final List<AiMessage> messages = new ArrayList<>();

        @Override
        public int insert(AiMessage message) {
            messages.add(message);
            return 1;
        }

        @Override
        public List<AiMessage> listBySessionId(String sessionId) {
            return messages.stream().filter(message -> message.getSessionId().equals(sessionId)).toList();
        }
    }
}
