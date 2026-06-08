package com.idealagent.domain.ai.service.chat;

import com.idealagent.domain.ai.model.dto.ChatRequestDTO;
import com.idealagent.domain.ai.model.entity.AiConfigRecord;
import com.idealagent.domain.ai.model.enumeration.ConfigKind;
import com.idealagent.domain.ai.model.vo.ChatClientOptionVO;
import com.idealagent.domain.ai.model.vo.ChatResponseVO;
import com.idealagent.domain.ai.repository.IAiConfigRepository;
import com.idealagent.domain.ai.service.augment.IAugmentService;
import com.idealagent.domain.ai.service.augment.IMcpToolService;
import com.idealagent.domain.ai.service.augment.McpToolHandle;
import com.idealagent.domain.ai.service.dispatch.IChatDispatchService;
import com.idealagent.domain.session.model.entity.ChatMessage;
import com.idealagent.domain.session.model.entity.ChatSession;
import com.idealagent.domain.session.repository.ISessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.tool.ToolCallbackProvider;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class ChatServiceTest {
    private FakeChatRepository repository;
    private FakeAiConfigRepository configRepository;
    private RecordingDispatchService dispatchService;
    private RecordingAugmentService augmentService;
    private RecordingMcpToolService mcpToolService;
    private RecordingChatGateway chatGateway;
    private ChatService chatService;

    @BeforeEach
    void setUp() {
        repository = new FakeChatRepository();
        configRepository = new FakeAiConfigRepository();
        dispatchService = new RecordingDispatchService();
        augmentService = new RecordingAugmentService();
        mcpToolService = new RecordingMcpToolService();
        chatGateway = new RecordingChatGateway();
        RuntimeMessageBuilder messageBuilder = new RuntimeMessageBuilder(repository, configRepository, augmentService);
        chatService = new ChatService(repository, configRepository, dispatchService, mcpToolService, chatGateway, messageBuilder);
    }

    @Test
    void sendCreatesSessionDispatchesSpringAiClientAndPersistsMessages() {
        chatGateway.completeResponse = "模型回复";

        ChatResponseVO response = chatService.send(7L, new ChatRequestDTO(null, "client_default_chat", "你好"));

        assertThat(response.sessionId()).startsWith("session_");
        assertThat(response.assistantMessage().role()).isEqualTo("assistant");
        assertThat(response.assistantMessage().content()).isEqualTo("模型回复");
        assertThat(dispatchService.lastClientId).isEqualTo("client_default_chat");
        assertThat(augmentService.lastUserId).isEqualTo(7L);
        assertThat(augmentService.lastUserMessage).isEqualTo("你好");
        assertThat(mcpToolService.lastUserId).isEqualTo(7L);
        assertThat(mcpToolService.lastClientId).isNull();
        assertThat(mcpToolService.lastMcpIds).isEmpty();
        assertThat(chatGateway.lastToolProvider).isSameAs(mcpToolService.handle.toolCallbackProvider());
        assertThat(mcpToolService.handle.closed).isTrue();
        assertThat(chatGateway.lastMessages).extracting(Message::getText).containsExactly("你好");
        assertThat(repository.messages).extracting(ChatMessage::getRole).containsExactly("user", "assistant");
    }

    @Test
    void sendPassesRagTagToAugmentService() {
        chatService.send(7L, new ChatRequestDTO(null, "client_default_chat", "怎么存向量？", "spring-ai"));

        assertThat(augmentService.lastRagTag).isEqualTo("spring-ai");
    }

    @Test
    void sendUsesRequestMcpIdsInsteadOfClientBindings() {
        chatService.send(7L, new ChatRequestDTO(null, "client_default_chat", "查天气", null, List.of("mcp_weather", "mcp_map")));

        assertThat(mcpToolService.lastUserId).isEqualTo(7L);
        assertThat(mcpToolService.lastClientId).isNull();
        assertThat(mcpToolService.lastMcpIds).containsExactly("mcp_weather", "mcp_map");
        assertThat(chatGateway.lastToolProvider).isSameAs(mcpToolService.handle.toolCallbackProvider());
    }

    @Test
    void sendIncludesSameSessionHistoryInModelMessages() {
        ChatSession session = session("session_existing", 7L, "client_default_chat");
        repository.saveSession(session);
        repository.saveMessage(message("session_existing", "user", "我叫张三"));
        repository.saveMessage(message("session_existing", "assistant", "你好，张三"));

        chatService.send(7L, new ChatRequestDTO("session_existing", "client_default_chat", "我叫什么？"));

        assertThat(chatGateway.lastMessages).extracting(Message::getText)
                .containsExactly("我叫张三", "你好，张三", "我叫什么？");
        assertThat(chatGateway.lastMessages.get(0)).isInstanceOf(UserMessage.class);
        assertThat(chatGateway.lastMessages.get(1)).isInstanceOf(AssistantMessage.class);
        assertThat(chatGateway.lastMessages.get(2)).isInstanceOf(UserMessage.class);
    }

    @Test
    void sendLeavesClientPromptOutOfRequestMessages() {
        configRepository.add(ConfigKind.CONFIG, configBinding("config_prompt", "client_default_chat", "prompt", "prompt_system", 1));
        configRepository.add(ConfigKind.PROMPT, promptRecord("prompt_system", "你是 IdealAgent，请用中文回答。", 1));

        chatService.send(7L, new ChatRequestDTO(null, "client_default_chat", "你好"));

        assertThat(chatGateway.lastMessages).extracting(Message::getText)
                .containsExactly("你好");
        assertThat(chatGateway.lastMessages.get(0)).isInstanceOf(UserMessage.class);
    }

    @Test
    void sendUsesMiniAgentMemoryAdvisorMaxMessagesToLimitHistoryWindow() {
        configRepository.add(ConfigKind.CONFIG, configBinding("config_memory", "client_default_chat", "advisor", "advisor_memory", 1));
        configRepository.add(ConfigKind.ADVISOR, advisorRecord("advisor_memory", "Memory", "{\"maxMessages\":3}", 1));
        repository.saveSession(session("session_memory", 7L, "client_default_chat"));
        repository.saveMessage(message("session_memory", "user", "u1"));
        repository.saveMessage(message("session_memory", "assistant", "a1"));
        repository.saveMessage(message("session_memory", "user", "u2"));
        repository.saveMessage(message("session_memory", "assistant", "a2"));

        chatService.send(7L, new ChatRequestDTO("session_memory", "client_default_chat", "u3"));

        assertThat(chatGateway.lastMessages).extracting(Message::getText)
                .containsExactly("a1", "u2", "a2", "u3");
    }

    @Test
    void sendIgnoresRagAdvisorWhenRequestHasNoRagTag() {
        configRepository.add(ConfigKind.CONFIG, configBinding("config_rag", "client_default_chat", "advisor", "advisor_rag", 1));
        configRepository.add(ConfigKind.ADVISOR, advisorRecord("advisor_rag", "Rag", "{\"topK\":4,\"filterExpression\":\"knowledge == 'project-docs'\"}", 1));

        chatService.send(7L, new ChatRequestDTO(null, "client_default_chat", "介绍项目"));

        assertThat(augmentService.lastRagTag).isNull();
        assertThat(augmentService.lastTopK).isNull();
        assertThat(augmentService.lastFilterExpression).isNull();
    }

    @Test
    void sendUsesRequestRagTagWithoutClientAdvisorSettings() {
        configRepository.add(ConfigKind.CONFIG, configBinding("config_rag", "client_default_chat", "advisor", "advisor_rag", 1));
        configRepository.add(ConfigKind.ADVISOR, advisorRecord("advisor_rag", "Rag", "{\"topK\":4,\"filterExpression\":\"source == 'note.md'\"}", 1));

        chatService.send(7L, new ChatRequestDTO(null, "client_default_chat", "介绍项目", "request-docs"));

        assertThat(augmentService.lastRagTag).isEqualTo("request-docs");
        assertThat(augmentService.lastTopK).isNull();
        assertThat(augmentService.lastFilterExpression).isNull();
    }

    @Test
    void streamEmitsDeltasAndPersistsFullAssistantMessage() {
        chatGateway.streamDeltas = List.of("你", "好");
        List<String> deltas = new ArrayList<>();

        ChatResponseVO response = chatService.stream(7L, new ChatRequestDTO(null, "client_default_chat", "你好"), deltas::add);

        assertThat(deltas).containsExactly("你", "好");
        assertThat(response.assistantMessage().content()).isEqualTo("你好");
        assertThat(dispatchService.lastClientId).isEqualTo("client_default_chat");
        assertThat(repository.messages).extracting(ChatMessage::getRole).containsExactly("user", "assistant");
    }

    @Test
    void sendRejectsBlankContent() {
        assertThatThrownBy(() -> chatService.send(7L, new ChatRequestDTO(null, "client_default_chat", " ")))
                .isInstanceOf(ChatException.class)
                .hasMessage("消息内容不能为空");
    }

    @Test
    void listClientsReturnsEnabledClientOptions() {
        configRepository.add(ConfigKind.CLIENT, clientRecord("client_default_chat", "Default Chat", "chat", "model_default_chat", "gpt-4o-mini", 1));
        configRepository.add(ConfigKind.CLIENT, clientRecord("client_runner", "Runner", "work", "model_default_chat", "gpt-4o-mini", 1));
        configRepository.add(ConfigKind.CLIENT, clientRecord("client_disabled", "Disabled", "chat", "model_disabled", "gpt-disabled", 0));

        List<ChatClientOptionVO> clients = chatService.listClients();

        assertThat(clients).hasSize(1);
        assertThat(clients.get(0).clientId()).isEqualTo("client_default_chat");
        assertThat(clients.get(0).clientType()).isEqualTo("chat");
        assertThat(clients.get(0).clientName()).isEqualTo("Default Chat");
        assertThat(clients.get(0).modelId()).isEqualTo("model_default_chat");
        assertThat(clients.get(0).modelName()).isEqualTo("gpt-4o-mini");
    }

    @Test
    void listMessagesRejectsSessionOwnedByAnotherUser() {
        repository.saveSession(session("session_private", 7L, "client_default_chat"));
        repository.saveMessage(message("session_private", "user", "secret"));

        assertThatThrownBy(() -> chatService.listMessages(8L, "session_private"))
                .isInstanceOf(ChatException.class)
                .hasMessage("会话不存在");
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

    private AiConfigRecord configBinding(String id, String clientId, String configType, String refId, Integer status) {
        AiConfigRecord record = new AiConfigRecord();
        record.setConfigId(id);
        record.setOwnerType("client");
        record.setContent(clientId);
        record.setConfigType(configType);
        record.setRefId(refId);
        record.setStatus(status);
        return record;
    }

    private AiConfigRecord promptRecord(String promptId, String content, Integer status) {
        AiConfigRecord record = new AiConfigRecord();
        record.setConfigId(promptId);
        record.setName(promptId);
        record.setType("system");
        record.setContent(content);
        record.setStatus(status);
        return record;
    }

    private AiConfigRecord advisorRecord(String advisorId, String type, String content, Integer status) {
        AiConfigRecord record = new AiConfigRecord();
        record.setConfigId(advisorId);
        record.setName(advisorId);
        record.setType(type);
        record.setContent(content);
        record.setStatus(status);
        return record;
    }

    private ChatSession session(String sessionId, Long userId, String targetId) {
        ChatSession session = new ChatSession();
        session.setSessionId(sessionId);
        session.setType("chat");
        session.setTitle("test");
        session.setUserId(userId);
        session.setTargetId(targetId);
        return session;
    }

    private ChatMessage message(String sessionId, String role, String content) {
        ChatMessage message = new ChatMessage();
        message.setMessageId("msg_" + repository.messages.size());
        message.setSessionId(sessionId);
        message.setType("chat");
        message.setRole(role);
        message.setContent(content);
        return message;
    }

    private static class FakeChatRepository implements ISessionRepository {
        private final List<ChatSession> sessions = new ArrayList<>();
        private final List<ChatMessage> messages = new ArrayList<>();

        @Override
        public Optional<ChatSession> findSession(String sessionId, Long userId) {
            return sessions.stream()
                    .filter(session -> session.getSessionId().equals(sessionId) && session.getUserId().equals(userId))
                    .findFirst();
        }

        @Override
        public Optional<ChatSession> findSession(String sessionId, Long userId, String type) {
            return findSession(sessionId, userId).filter(session -> type.equals(session.getType()));
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
        public List<ChatSession> listSessions(Long userId, String type) {
            return sessions.stream().filter(session -> session.getUserId().equals(userId) && type.equals(session.getType())).toList();
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

    private static class FakeAiConfigRepository implements IAiConfigRepository {
        private final Map<ConfigKind, List<AiConfigRecord>> records = new EnumMap<>(ConfigKind.class);

        void add(ConfigKind kind, AiConfigRecord record) {
            records.computeIfAbsent(kind, ignored -> new ArrayList<>()).add(record);
        }

        @Override
        public AiConfigRecord save(ConfigKind kind, AiConfigRecord record) {
            add(kind, record);
            return record;
        }

        @Override
        public List<AiConfigRecord> list(ConfigKind kind) {
            return records.getOrDefault(kind, List.of());
        }

        @Override
        public AiConfigRecord update(ConfigKind kind, AiConfigRecord record) {
            return record;
        }

        @Override
        public void updateStatus(ConfigKind kind, String configId, Integer status) {
        }

        @Override
        public void delete(ConfigKind kind, String configId) {
        }
    }

    private static class RecordingDispatchService implements IChatDispatchService {
        private String lastClientId;
        private final ChatClient chatClient = mock(ChatClient.class);

        @Override
        public ChatClient dispatchChatClient(String clientId) {
            lastClientId = clientId;
            return chatClient;
        }
    }

    private static class RecordingAugmentService implements IAugmentService {
        private Long lastUserId;
        private String lastUserMessage;
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
            lastUserId = userId;
            lastUserMessage = userMessage;
            lastRagTag = ragTag;
            lastTopK = topK;
            lastFilterExpression = filterExpression;
            return List.of(new UserMessage(userMessage));
        }
    }

    private static class RecordingMcpToolService implements IMcpToolService {
        private Long lastUserId;
        private String lastClientId;
        private List<String> lastMcpIds;
        private final RecordingMcpToolHandle handle = new RecordingMcpToolHandle();

        @Override
        public McpToolHandle augmentMcpTool(Long userId, String clientId) {
            lastUserId = userId;
            lastClientId = clientId;
            lastMcpIds = null;
            return handle;
        }

        @Override
        public McpToolHandle augmentMcpTool(Long userId, List<String> mcpIds) {
            lastUserId = userId;
            lastClientId = null;
            lastMcpIds = mcpIds;
            return handle;
        }
    }

    private static class RecordingMcpToolHandle extends McpToolHandle {
        private boolean closed;

        RecordingMcpToolHandle() {
            super(new SyncMcpToolCallbackProvider(), List.of());
        }

        @Override
        public void close() {
            closed = true;
            super.close();
        }
    }

    private static class RecordingChatGateway extends SpringAiChatGateway {
        private String completeResponse = "模型回复";
        private List<String> streamDeltas = List.of();
        private List<Message> lastMessages = List.of();
        private ToolCallbackProvider lastToolProvider;

        @Override
        public String complete(ChatClient chatClient, List<Message> messages) {
            lastMessages = List.copyOf(messages);
            return completeResponse;
        }

        @Override
        public String complete(ChatClient chatClient, List<Message> messages, ToolCallbackProvider toolCallbackProvider) {
            lastMessages = List.copyOf(messages);
            lastToolProvider = toolCallbackProvider;
            return completeResponse;
        }

        @Override
        public void stream(ChatClient chatClient, List<Message> messages, Consumer<String> onDelta) {
            lastMessages = List.copyOf(messages);
            streamDeltas.forEach(onDelta);
        }

        @Override
        public void stream(ChatClient chatClient, List<Message> messages, ToolCallbackProvider toolCallbackProvider, Consumer<String> onDelta) {
            lastMessages = List.copyOf(messages);
            lastToolProvider = toolCallbackProvider;
            streamDeltas.forEach(onDelta);
        }
    }
}
