package com.idealagent.domain.chat.service;

import com.idealagent.domain.chat.model.dto.ChatRequestDTO;
import com.idealagent.domain.chat.model.entity.ChatMessage;
import com.idealagent.domain.chat.model.entity.ChatSession;
import com.idealagent.domain.chat.model.vo.ChatClientOptionVO;
import com.idealagent.domain.chat.model.vo.ChatMessageVO;
import com.idealagent.domain.chat.model.vo.ChatResponseVO;
import com.idealagent.domain.chat.model.vo.ChatSessionVO;
import com.idealagent.domain.chat.repository.IChatRepository;
import com.idealagent.domain.config.model.entity.AiConfigRecord;
import com.idealagent.domain.config.repository.IAiConfigRepository;
import com.idealagent.domain.config.service.ConfigKind;
import com.idealagent.domain.rag.model.entity.RagChunk;
import com.idealagent.domain.rag.service.RagService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class ChatService {
    private static final String CHAT_TYPE = "chat";
    private static final String USER_ROLE = "user";
    private static final String SYSTEM_ROLE = "system";
    private static final String ASSISTANT_ROLE = "assistant";
    private static final String LOCAL_ECHO_CLIENT = "local_echo";
    private static final int ENABLED = 1;

    private final IChatRepository chatRepository;
    private final IChatClient chatClient;
    private final IAiConfigRepository aiConfigRepository;
    private final RagService ragService;

    public ChatService(IChatRepository chatRepository, IChatClient chatClient, IAiConfigRepository aiConfigRepository, RagService ragService) {
        this.chatRepository = chatRepository;
        this.chatClient = chatClient;
        this.aiConfigRepository = aiConfigRepository;
        this.ragService = ragService;
    }

    public ChatResponseVO send(Long userId, ChatRequestDTO request) {
        validate(userId, request);
        String sessionId = StringUtils.hasText(request.sessionId()) ? request.sessionId() : id("session_");
        String clientId = StringUtils.hasText(request.clientId()) ? request.clientId() : LOCAL_ECHO_CLIENT;
        ensureSession(userId, sessionId, request);

        ChatMessage userMessage = message(sessionId, USER_ROLE, request.content());
        chatRepository.saveMessage(userMessage);

        List<ChatMessage> history = new ArrayList<>(chatRepository.listMessages(sessionId, userId));
        List<ChatMessage> promptHistory = withRagContext(userId, request, history);
        String assistantContent = chatClient.complete(clientId, promptHistory);
        ChatMessage assistantMessage = chatRepository.saveMessage(message(sessionId, ASSISTANT_ROLE, assistantContent));

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

    private List<ChatMessage> withRagContext(Long userId, ChatRequestDTO request, List<ChatMessage> history) {
        if (!StringUtils.hasText(request.ragTag()) || ragService == null) {
            return history;
        }
        List<RagChunk> chunks = ragService.retrieve(userId, request.ragTag(), request.content());
        if (chunks.isEmpty()) {
            return history;
        }
        List<ChatMessage> augmented = new ArrayList<>();
        augmented.add(message("rag_context", SYSTEM_ROLE, ragContext(chunks)));
        augmented.addAll(history);
        return augmented;
    }

    private String ragContext(List<RagChunk> chunks) {
        StringBuilder builder = new StringBuilder("Use the following knowledge context to answer:\n");
        for (RagChunk chunk : chunks) {
            builder.append("\nSource: ").append(chunk.source()).append("\n").append(chunk.content()).append('\n');
        }
        return builder.toString();
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
