package com.idealagent.domain.ai.service.chat;

import com.idealagent.domain.ai.model.entity.AiConfigRecord;
import com.idealagent.domain.ai.model.enumeration.ConfigKind;
import com.idealagent.domain.ai.repository.IAiConfigRepository;
import com.idealagent.domain.ai.service.augment.IAugmentService;
import com.idealagent.domain.session.model.entity.ChatMessage;
import com.idealagent.domain.session.model.entity.ChatSession;
import com.idealagent.domain.session.repository.ISessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class RuntimeMessageBuilderTest {
    private FakeSessionRepository sessionRepository;
    private FakeAiConfigRepository configRepository;
    private RecordingAugmentService augmentService;
    private RuntimeMessageBuilder builder;

    @BeforeEach
    void setUp() {
        sessionRepository = new FakeSessionRepository();
        configRepository = new FakeAiConfigRepository();
        augmentService = new RecordingAugmentService();
        builder = new RuntimeMessageBuilder(sessionRepository, configRepository, augmentService);
    }

    @Test
    void buildsMessagesWithMemoryWindowForGivenMessageType() {
        configRepository.add(ConfigKind.CONFIG, binding("config_memory", "client_work", "advisor", "advisor_memory", 1));
        configRepository.add(ConfigKind.ADVISOR, advisor("advisor_memory", "Memory", "{\"maxMessages\":2}", 1));
        sessionRepository.saveSession(session("session_work", 7L, "work"));
        sessionRepository.saveMessage(message("session_work", "work", "user", "u1"));
        sessionRepository.saveMessage(message("session_work", "work", "assistant", "a1"));
        sessionRepository.saveMessage(message("session_work", "work", "user", "u2"));

        List<Message> messages = builder.build(7L, "session_work", "client_work", "node prompt", null, "work");

        assertThat(messages).extracting(Message::getText).containsExactly("a1", "u2", "node prompt");
        assertThat(messages.get(0)).isInstanceOf(AssistantMessage.class);
        assertThat(messages.get(1)).isInstanceOf(UserMessage.class);
        assertThat(messages.get(2)).isInstanceOf(UserMessage.class);
    }

    @Test
    void appliesRagAdvisorParametersOnlyWhenRagTagIsProvided() {
        configRepository.add(ConfigKind.CONFIG, binding("config_rag", "client_work", "advisor", "advisor_rag", 1));
        configRepository.add(ConfigKind.ADVISOR, advisor("advisor_rag", "Rag", "{\"ragTag\":\"ignored\",\"topK\":4,\"filterExpression\":\"source == 'note.md'\"}", 1));

        List<Message> withoutRag = builder.build(7L, "session_work", "client_work", "node prompt", null, "work");
        assertThat(augmentService.lastRagTag).isNull();
        assertThat(augmentService.lastTopK).isNull();
        assertThat(withoutRag).extracting(Message::getText).containsExactly("node prompt");

        List<Message> withRag = builder.build(7L, "session_work", "client_work", "node prompt", "project-docs", "work");
        assertThat(augmentService.lastRagTag).isEqualTo("project-docs");
        assertThat(augmentService.lastTopK).isEqualTo(4);
        assertThat(augmentService.lastFilterExpression).isEqualTo("source == 'note.md'");
        assertThat(withRag).extracting(Message::getText).containsExactly("RAG:project-docs", "node prompt");
        assertThat(withRag.get(0)).isInstanceOf(SystemMessage.class);
    }

    private AiConfigRecord binding(String id, String clientId, String configType, String refId, Integer status) {
        AiConfigRecord record = new AiConfigRecord();
        record.setConfigId(id);
        record.setOwnerType("client");
        record.setContent(clientId);
        record.setConfigType(configType);
        record.setRefId(refId);
        record.setStatus(status);
        return record;
    }

    private AiConfigRecord advisor(String id, String type, String content, Integer status) {
        AiConfigRecord record = new AiConfigRecord();
        record.setConfigId(id);
        record.setType(type);
        record.setContent(content);
        record.setStatus(status);
        return record;
    }

    private ChatSession session(String sessionId, Long userId, String type) {
        ChatSession session = new ChatSession();
        session.setSessionId(sessionId);
        session.setUserId(userId);
        session.setType(type);
        return session;
    }

    private ChatMessage message(String sessionId, String type, String role, String content) {
        ChatMessage message = new ChatMessage();
        message.setMessageId("msg_" + sessionRepository.messages.size());
        message.setSessionId(sessionId);
        message.setType(type);
        message.setRole(role);
        message.setContent(content);
        return message;
    }

    private static class RecordingAugmentService implements IAugmentService {
        private String lastRagTag;
        private Integer lastTopK;
        private String lastFilterExpression;

        @Override
        public List<Message> augmentRagMessage(Long userId, String userMessage, String ragTag) {
            return augmentRagMessage(userId, userMessage, ragTag, null, null);
        }

        @Override
        public List<Message> augmentRagMessage(Long userId, String userMessage, String ragTag, Integer topK) {
            return augmentRagMessage(userId, userMessage, ragTag, topK, null);
        }

        @Override
        public List<Message> augmentRagMessage(Long userId, String userMessage, String ragTag, Integer topK, String filterExpression) {
            lastRagTag = ragTag;
            lastTopK = topK;
            lastFilterExpression = filterExpression;
            if (ragTag == null || ragTag.isBlank()) {
                return List.of(new UserMessage(userMessage));
            }
            return List.of(new SystemMessage("RAG:" + ragTag), new UserMessage(userMessage));
        }
    }

    private static class FakeAiConfigRepository implements IAiConfigRepository {
        private final Map<ConfigKind, List<AiConfigRecord>> records = new EnumMap<>(ConfigKind.class);

        void add(ConfigKind kind, AiConfigRecord record) {
            records.computeIfAbsent(kind, ignored -> new ArrayList<>()).add(record);
        }

        @Override public AiConfigRecord save(ConfigKind kind, AiConfigRecord record) { add(kind, record); return record; }
        @Override public List<AiConfigRecord> list(ConfigKind kind) { return records.getOrDefault(kind, List.of()); }
        @Override public AiConfigRecord find(ConfigKind kind, String configId) {
            return list(kind).stream().filter(record -> configId.equals(record.getConfigId())).findFirst().orElse(null);
        }
        @Override public AiConfigRecord update(ConfigKind kind, AiConfigRecord record) { return record; }
        @Override public void updateStatus(ConfigKind kind, String configId, Integer status) { }
        @Override public void delete(ConfigKind kind, String configId) { }
    }

    private static class FakeSessionRepository implements ISessionRepository {
        private final List<ChatSession> sessions = new ArrayList<>();
        private final List<ChatMessage> messages = new ArrayList<>();

        @Override public Optional<ChatSession> findSession(String sessionId, Long userId) {
            return sessions.stream().filter(session -> session.getSessionId().equals(sessionId) && session.getUserId().equals(userId)).findFirst();
        }
        @Override public Optional<ChatSession> findSession(String sessionId, Long userId, String type) {
            return findSession(sessionId, userId).filter(session -> type.equals(session.getType()));
        }
        @Override public ChatSession saveSession(ChatSession session) { sessions.add(session); return session; }
        @Override public List<ChatSession> listSessions(Long userId) { return sessions.stream().filter(session -> session.getUserId().equals(userId)).toList(); }
        @Override public List<ChatSession> listSessions(Long userId, String type) { return sessions.stream().filter(session -> session.getUserId().equals(userId) && type.equals(session.getType())).toList(); }
        @Override public ChatMessage saveMessage(ChatMessage message) { messages.add(message); return message; }
        @Override public List<ChatMessage> listMessages(String sessionId, Long userId) { return messages.stream().filter(message -> message.getSessionId().equals(sessionId)).toList(); }
    }
}
