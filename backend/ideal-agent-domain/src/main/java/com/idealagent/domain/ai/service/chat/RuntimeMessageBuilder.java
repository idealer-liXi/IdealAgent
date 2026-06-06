package com.idealagent.domain.ai.service.chat;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.idealagent.domain.ai.model.entity.AiConfigRecord;
import com.idealagent.domain.ai.model.enumeration.ConfigKind;
import com.idealagent.domain.ai.repository.IAiConfigRepository;
import com.idealagent.domain.ai.service.augment.IAugmentService;
import com.idealagent.domain.session.model.entity.ChatMessage;
import com.idealagent.domain.session.repository.ISessionRepository;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class RuntimeMessageBuilder {
    private static final String USER_ROLE = "user";
    private static final String SYSTEM_ROLE = "system";
    private static final String ASSISTANT_ROLE = "assistant";
    private static final int ENABLED = 1;
    private static final int DEFAULT_HISTORY_RETRIEVE_SIZE = 10;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final ISessionRepository sessionRepository;
    private final IAiConfigRepository aiConfigRepository;
    private final IAugmentService augmentService;

    public RuntimeMessageBuilder(ISessionRepository sessionRepository, IAiConfigRepository aiConfigRepository, IAugmentService augmentService) {
        this.sessionRepository = sessionRepository;
        this.aiConfigRepository = aiConfigRepository;
        this.augmentService = augmentService;
    }

    public List<Message> build(Long userId, String sessionId, String clientId, String content, String ragTag, String messageType) {
        List<Message> messages = new ArrayList<>();
        Integer ragTopK = StringUtils.hasText(ragTag) ? effectiveRagTopK(clientId) : null;
        String ragFilterExpression = StringUtils.hasText(ragTag) ? effectiveRagFilterExpression(clientId) : null;
        List<Message> augmentedMessages = augmentService.augmentRagMessage(userId, content, ragTag, ragTopK, ragFilterExpression);
        List<Message> currentUserMessages = new ArrayList<>();
        for (Message message : augmentedMessages) {
            if (message instanceof UserMessage) {
                currentUserMessages.add(message);
            } else {
                messages.add(message);
            }
        }
        messages.addAll(historyMessages(userId, sessionId, clientId, messageType));
        if (currentUserMessages.isEmpty()) {
            messages.add(new UserMessage(content));
        } else {
            messages.addAll(currentUserMessages);
        }
        return messages;
    }

    private List<Message> historyMessages(Long userId, String sessionId, String clientId, String messageType) {
        List<Message> history = sessionRepository.listMessages(sessionId, userId).stream()
                .filter(message -> messageType.equals(message.getType()))
                .map(this::toSpringMessage)
                .filter(message -> message != null)
                .toList();
        int retrieveSize = historyRetrieveSize(clientId);
        if (history.size() <= retrieveSize) {
            return history;
        }
        return history.subList(history.size() - retrieveSize, history.size());
    }

    private Message toSpringMessage(ChatMessage message) {
        if (USER_ROLE.equals(message.getRole())) {
            return new UserMessage(message.getContent());
        }
        if (ASSISTANT_ROLE.equals(message.getRole())) {
            return new AssistantMessage(message.getContent());
        }
        if (SYSTEM_ROLE.equals(message.getRole())) {
            return new SystemMessage(message.getContent());
        }
        return null;
    }

    private int historyRetrieveSize(String clientId) {
        return clientBindings(clientId, "advisor").stream()
                .map(binding -> aiConfigRepository.find(ConfigKind.ADVISOR, binding.getRefId()))
                .filter(this::enabled)
                .filter(advisor -> typeEquals(advisor, "memory"))
                .map(AiConfigRecord::getContent)
                .map(content -> configInt(content, DEFAULT_HISTORY_RETRIEVE_SIZE, "maxMessages", "retrieveSize"))
                .filter(size -> size > 0)
                .findFirst()
                .orElse(DEFAULT_HISTORY_RETRIEVE_SIZE);
    }

    private Integer effectiveRagTopK(String clientId) {
        return ragAdvisorContent(clientId)
                .map(content -> configInt(content, 0, "topK"))
                .filter(size -> size > 0)
                .findFirst()
                .orElse(null);
    }

    private String effectiveRagFilterExpression(String clientId) {
        return ragAdvisorContent(clientId)
                .map(content -> configString(content, "filterExpression"))
                .filter(StringUtils::hasText)
                .findFirst()
                .orElse(null);
    }

    private java.util.stream.Stream<String> ragAdvisorContent(String clientId) {
        return clientBindings(clientId, "advisor").stream()
                .map(binding -> aiConfigRepository.find(ConfigKind.ADVISOR, binding.getRefId()))
                .filter(this::enabled)
                .filter(advisor -> typeEquals(advisor, "rag"))
                .map(AiConfigRecord::getContent);
    }

    private List<AiConfigRecord> clientBindings(String clientId, String configType) {
        return aiConfigRepository.list(ConfigKind.CONFIG).stream()
                .filter(this::enabled)
                .filter(record -> "client".equals(record.getOwnerType()))
                .filter(record -> clientId.equals(record.getContent()))
                .filter(record -> configType.equals(record.getConfigType()))
                .filter(record -> StringUtils.hasText(record.getRefId()))
                .toList();
    }

    private int configInt(String content, int fallback, String... keys) {
        try {
            Map<String, Object> config = OBJECT_MAPPER.readValue(content == null ? "{}" : content, new TypeReference<>() {
            });
            for (String key : keys) {
                Object value = config.get(key);
                if (value instanceof Number number) {
                    return number.intValue();
                }
                if (value instanceof String text && StringUtils.hasText(text)) {
                    return Integer.parseInt(text);
                }
            }
        } catch (Exception ignored) {
            return fallback;
        }
        return fallback;
    }

    private String configString(String content, String key) {
        try {
            Map<String, Object> config = OBJECT_MAPPER.readValue(content == null ? "{}" : content, new TypeReference<>() {
            });
            Object value = config.get(key);
            return value == null ? null : value.toString();
        } catch (Exception ignored) {
            return null;
        }
    }

    private boolean enabled(AiConfigRecord record) {
        return record != null && record.getStatus() != null && record.getStatus() == ENABLED;
    }

    private boolean typeEquals(AiConfigRecord record, String type) {
        return record != null && record.getType() != null && record.getType().equalsIgnoreCase(type);
    }
}
