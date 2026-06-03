package com.idealagent.domain.ai.service.chat;

import com.idealagent.domain.ai.model.dto.ChatRequestDTO;
import com.idealagent.domain.ai.service.augment.IAugmentService;
import com.idealagent.domain.ai.service.augment.IMcpToolService;
import com.idealagent.domain.ai.service.augment.McpToolHandle;
import com.idealagent.domain.ai.service.dispatch.IChatDispatchService;
import com.idealagent.domain.session.model.entity.ChatMessage;
import com.idealagent.domain.session.model.entity.ChatSession;
import com.idealagent.domain.ai.model.vo.ChatClientOptionVO;
import com.idealagent.domain.session.model.vo.ChatMessageVO;
import com.idealagent.domain.ai.model.vo.ChatResponseVO;
import com.idealagent.domain.session.model.vo.ChatSessionVO;
import com.idealagent.domain.session.repository.ISessionRepository;
import com.idealagent.domain.ai.model.entity.AiConfigRecord;
import com.idealagent.domain.ai.repository.IAiConfigRepository;
import com.idealagent.domain.ai.model.enumeration.ConfigKind;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

@Service
public class ChatService {
    private static final String CHAT_TYPE = "chat";
    private static final String USER_ROLE = "user";
    private static final String SYSTEM_ROLE = "system";
    private static final String ASSISTANT_ROLE = "assistant";
    private static final String LOCAL_ECHO_CLIENT = "local_echo";
    private static final int ENABLED = 1;

    private final ISessionRepository chatRepository;
    private final IAiConfigRepository aiConfigRepository;
    private final IChatDispatchService chatDispatchService;
    private final IAugmentService augmentService;
    private final IMcpToolService mcpToolService;
    private final SpringAiChatGateway chatGateway;

    public ChatService(ISessionRepository chatRepository,
                       IAiConfigRepository aiConfigRepository,
                       IChatDispatchService chatDispatchService,
                       IAugmentService augmentService,
                       IMcpToolService mcpToolService,
                       SpringAiChatGateway chatGateway) {
        this.chatRepository = chatRepository;
        this.aiConfigRepository = aiConfigRepository;
        this.chatDispatchService = chatDispatchService;
        this.augmentService = augmentService;
        this.mcpToolService = mcpToolService;
        this.chatGateway = chatGateway;
    }

    public ChatResponseVO send(Long userId, ChatRequestDTO request) {
        validate(userId, request);
        String sessionId = StringUtils.hasText(request.sessionId()) ? request.sessionId() : id("session_");
        String clientId = StringUtils.hasText(request.clientId()) ? request.clientId() : LOCAL_ECHO_CLIENT;
        ensureSession(userId, sessionId, request);

        ChatMessage userMessage = message(sessionId, USER_ROLE, request.content());
        chatRepository.saveMessage(userMessage);

        ChatClient runtimeClient = chatDispatchService.dispatchChatClient(clientId);
        List<Message> messages = augmentService.augmentRagMessage(userId, request.content(), request.ragTag());
        try (McpToolHandle mcpTools = mcpToolService.augmentMcpTool(userId, clientId)) {
            String assistantContent = chatGateway.complete(runtimeClient, messages, mcpTools.toolCallbackProvider());
            ChatMessage assistantMessage = chatRepository.saveMessage(message(sessionId, ASSISTANT_ROLE, assistantContent));
            return new ChatResponseVO(sessionId, toMessageVo(assistantMessage));
        }
    }

    public ChatResponseVO stream(Long userId, ChatRequestDTO request, Consumer<String> onDelta) {
        validate(userId, request);
        String sessionId = StringUtils.hasText(request.sessionId()) ? request.sessionId() : id("session_");
        String clientId = StringUtils.hasText(request.clientId()) ? request.clientId() : LOCAL_ECHO_CLIENT;
        ensureSession(userId, sessionId, request);

        ChatMessage userMessage = message(sessionId, USER_ROLE, request.content());
        chatRepository.saveMessage(userMessage);

        ChatClient runtimeClient = chatDispatchService.dispatchChatClient(clientId);
        List<Message> messages = augmentService.augmentRagMessage(userId, request.content(), request.ragTag());
        StringBuilder assistantContent = new StringBuilder();
        try (McpToolHandle mcpTools = mcpToolService.augmentMcpTool(userId, clientId)) {
            chatGateway.stream(runtimeClient, messages, mcpTools.toolCallbackProvider(), delta -> {
                assistantContent.append(delta);
                onDelta.accept(delta);
            });
        }
        ChatMessage assistantMessage = chatRepository.saveMessage(message(sessionId, ASSISTANT_ROLE, assistantContent.toString()));

        return new ChatResponseVO(sessionId, toMessageVo(assistantMessage));
    }

    public List<ChatSessionVO> listSessions(Long userId) {
        return chatRepository.listSessions(userId).stream().map(this::toSessionVo).toList();
    }

    public List<ChatMessageVO> listMessages(Long userId, String sessionId) {
        return chatRepository.listMessages(sessionId, userId).stream().map(this::toMessageVo).toList();
    }

    public List<ChatClientOptionVO> listClients() {
        return aiConfigRepository.list(ConfigKind.CLIENT).stream()
                .filter(record -> record.getStatus() != null && record.getStatus() == ENABLED)
                .map(this::toClientOption)
                .toList();
    }

    private void validate(Long userId, ChatRequestDTO request) {
        if (userId == null) {
            throw new ChatException("用户未登录");
        }
        if (!StringUtils.hasText(request.content())) {
            throw new ChatException("消息内容不能为空");
        }
    }

    private void ensureSession(Long userId, String sessionId, ChatRequestDTO request) {
        if (chatRepository.findSession(sessionId, userId).isPresent()) {
            return;
        }
        ChatSession session = new ChatSession();
        session.setSessionId(sessionId);
        session.setType(CHAT_TYPE);
        session.setTitle(titleOf(request.content()));
        session.setUserId(userId);
        session.setTargetId(StringUtils.hasText(request.clientId()) ? request.clientId() : LOCAL_ECHO_CLIENT);
        chatRepository.saveSession(session);
    }

    private ChatMessage message(String sessionId, String role, String content) {
        ChatMessage message = new ChatMessage();
        message.setMessageId(id("msg_"));
        message.setSessionId(sessionId);
        message.setType(CHAT_TYPE);
        message.setRole(role);
        message.setContent(content);
        message.setCreateTime(LocalDateTime.now());
        return message;
    }

    private ChatMessageVO toMessageVo(ChatMessage message) {
        return new ChatMessageVO(message.getMessageId(), message.getSessionId(), message.getRole(), message.getContent(), message.getCreateTime());
    }

    private ChatSessionVO toSessionVo(ChatSession session) {
        return new ChatSessionVO(session.getSessionId(), session.getTitle(), session.getTargetId(), session.getCreateTime(), session.getUpdateTime());
    }

    private ChatClientOptionVO toClientOption(AiConfigRecord record) {
        return new ChatClientOptionVO(
                record.getConfigId(),
                record.getName(),
                record.getType(),
                record.getRefId(),
                record.getSecret(),
                record.getStatus());
    }

    private String titleOf(String content) {
        String trimmed = content.trim();
        return trimmed.length() > 24 ? trimmed.substring(0, 24) : trimmed;
    }

    private String id(String prefix) {
        return prefix + UUID.randomUUID().toString().replace("-", "");
    }
}
