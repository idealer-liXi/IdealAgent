package com.idealagent.domain.chat.service;

import com.idealagent.domain.chat.model.dto.ChatRequestDTO;
import com.idealagent.domain.chat.model.entity.ChatMessage;
import com.idealagent.domain.chat.model.entity.ChatSession;
import com.idealagent.domain.chat.model.vo.ChatClientOptionVO;
import com.idealagent.domain.chat.model.vo.ChatResponseVO;
import com.idealagent.domain.chat.repository.IChatRepository;
import com.idealagent.domain.config.model.entity.AiConfigRecord;
import com.idealagent.domain.config.repository.IAiConfigRepository;
import com.idealagent.domain.config.service.ConfigKind;
import com.idealagent.domain.rag.model.entity.RagChunk;
import com.idealagent.domain.rag.repository.IRagRepository;
import com.idealagent.domain.rag.service.DeterministicEmbeddingService;
import com.idealagent.domain.rag.service.RagService;
import com.idealagent.domain.rag.service.SimpleTextSplitter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ChatServiceTest {
    private FakeChatRepository repository;
    private FakeAiConfigRepository configRepository;
    private FakeRagRepository ragRepository;
    private RecordingChatClient chatClient;
    private ChatService chatService;

    @BeforeEach
    void setUp() {
        repository = new FakeChatRepository();
        configRepository = new FakeAiConfigRepository();
        ragRepository = new FakeRagRepository();
        chatClient = new RecordingChatClient();
        chatService = new ChatService(repository, chatClient, configRepository,
                new RagService(ragRepository, new SimpleTextSplitter(), new DeterministicEmbeddingService(), null));
    }

    @Test
    void sendCreatesSessionAndPersistsUserAndAssistantMessages() {
        ChatResponseVO response = chatService.send(7L, new ChatRequestDTO(null, "client_default_chat", "你好"));

        assertThat(response.sessionId()).startsWith("session_");
        assertThat(response.assistantMessage().role()).isEqualTo("assistant");
        assertThat(response.assistantMessage().content()).isEqualTo("Echo: 你好");
        assertThat(repository.sessions).hasSize(1);
        assertThat(repository.messages).extracting(ChatMessage::getRole).containsExactly("user", "assistant");
    }

    @Test
    void sendReusesExistingSessionHistory() {
        chatService.send(7L, new ChatRequestDTO("session_fixed", "client_default_chat", "第一句"));
        chatService.send(7L, new ChatRequestDTO("session_fixed", "client_default_chat", "第二句"));

        assertThat(chatClient.lastHistory).extracting(ChatMessage::getRole).containsExactly("user", "assistant", "user");
        assertThat(repository.messages).hasSize(4);
    }

    @Test
    void sendRejectsBlankContent() {
        assertThatThrownBy(() -> chatService.send(7L, new ChatRequestDTO(null, "client_default_chat", " ")))
                .isInstanceOf(ChatException.class)
                .hasMessage("消息内容不能为空");
    }

    @Test
    void sendFallsBackToLocalEchoWhenClientIdIsBlank() {
        ChatResponseVO response = chatService.send(7L, new ChatRequestDTO(null, "", "你好"));

        assertThat(chatClient.lastClientId).isEqualTo("local_echo");
        assertThat(response.assistantMessage().content()).isEqualTo("Echo: 你好");
    }

    @Test
    void sendAddsRagContextWhenRagTagProvided() {
        ragRepository.searchResults = List.of(new RagChunk("pgvector stores embeddings", "note.md", new float[1024]));

        chatService.send(7L, new ChatRequestDTO(null, "client_default_chat", "怎么存向量？", "spring-ai"));

        assertThat(ragRepository.searchTag).isEqualTo("spring-ai");
        assertThat(chatClient.lastHistory).extracting(ChatMessage::getRole).containsExactly("system", "user");
        assertThat(chatClient.lastHistory.get(0).getContent()).contains("pgvector stores embeddings");
    }

    @Test
    void listClientsReturnsEnabledClientOptions() {
        configRepository.records.add(clientRecord("client_default_chat", "Default Chat", "chat", "model_default_chat", "gpt-4o-mini", 1));
        configRepository.records.add(clientRecord("client_disabled", "Disabled", "chat", "model_disabled", "gpt-disabled", 0));

        List<ChatClientOptionVO> clients = chatService.listClients();

        assertThat(clients).hasSize(1);
        assertThat(clients.get(0).clientId()).isEqualTo("client_default_chat");
        assertThat(clients.get(0).clientName()).isEqualTo("Default Chat");
        assertThat(clients.get(0).modelId()).isEqualTo("model_default_chat");
        assertThat(clients.get(0).modelName()).isEqualTo("gpt-4o-mini");
    }

    private AiConfigRecord clientRecord(String clientId, String name, String type, String modelId, String modelName, Integer status) {
        AiConfigRecord record = new AiConfigRecord();
        record.setConfigId(clientId);
        record.setName(name);
        record.setType(type);
        record.setRefId(modelId);
        record.setSecret(modelName);
        record.setStatus(status);
        return record;
    }

    private static class FakeChatRepository implements IChatRepository {
        private final List<ChatSession> sessions = new ArrayList<>();
        private final List<ChatMessage> messages = new ArrayList<>();

        @Override
        public Optional<ChatSession> findSession(String sessionId, Long userId) {
            return sessions.stream()
                    .filter(session -> session.getSessionId().equals(sessionId) && session.getUserId().equals(userId))
                    .findFirst();
        }

        @Override
        public ChatSession saveSession(ChatSession session) {
            if (findSession(session.getSessionId(), session.getUserId()).isEmpty()) {
                sessions.add(session);
            }
            return session;
        }

        @Override
        public List<ChatSession> listSessions(Long userId) {
            return sessions.stream().filter(session -> session.getUserId().equals(userId)).toList();
        }

        @Override
        public ChatMessage saveMessage(ChatMessage message) {
            messages.add(message);
            return message;
        }

        @Override
        public List<ChatMessage> listMessages(String sessionId, Long userId) {
            return messages.stream().filter(message -> message.getSessionId().equals(sessionId)).toList();
        }
    }

    private static class RecordingChatClient implements IChatClient {
        private List<ChatMessage> lastHistory = List.of();
        private String lastClientId;

        @Override
        public String complete(String clientId, List<ChatMessage> history) {
            lastClientId = clientId;
            lastHistory = List.copyOf(history);
            return "Echo: " + history.get(history.size() - 1).getContent();
        }
    }

    private static class FakeAiConfigRepository implements IAiConfigRepository {
        private final List<AiConfigRecord> records = new ArrayList<>();

        @Override
        public AiConfigRecord save(ConfigKind kind, AiConfigRecord record) {
            records.add(record);
            return record;
        }

        @Override
        public List<AiConfigRecord> list(ConfigKind kind) {
            return kind == ConfigKind.CLIENT ? records : List.of();
        }
    }

    private static class FakeRagRepository implements IRagRepository {
        private String searchTag;
        private List<RagChunk> searchResults = List.of();

        @Override
        public void saveChunks(Long userId, String ragTag, List<RagChunk> chunks) {
        }

        @Override
        public List<String> listTags(Long userId) {
            return List.of();
        }

        @Override
        public List<RagChunk> search(Long userId, String ragTag, float[] queryEmbedding, int limit) {
            searchTag = ragTag;
            return searchResults;
        }
    }
}
