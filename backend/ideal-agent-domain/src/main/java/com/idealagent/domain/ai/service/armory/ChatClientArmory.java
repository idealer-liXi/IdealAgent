package com.idealagent.domain.ai.service.armory;

import com.idealagent.domain.ai.model.entity.AiConfigRecord;
import com.idealagent.domain.ai.model.enumeration.ConfigKind;
import com.idealagent.domain.ai.repository.IAiConfigRepository;
import com.idealagent.domain.ai.service.chat.ChatException;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ChatClientArmory implements IChatClientArmory {
    private static final int ENABLED = 1;
    private static final String DEFAULT_SYSTEM = "You are a helpful assistant.";

    private final IAiConfigRepository aiConfigRepository;
    private final Map<String, ChatClient> cache = new ConcurrentHashMap<>();

    public ChatClientArmory(IAiConfigRepository aiConfigRepository) {
        this.aiConfigRepository = aiConfigRepository;
    }

    @Override
    public ChatClient resolve(String clientId) {
        if (!StringUtils.hasText(clientId)) {
            throw new ChatException("Client 不可用");
        }
        return cache.computeIfAbsent(clientId, this::buildClient);
    }

    private ChatClient buildClient(String clientId) {
        AiConfigRecord client = requireEnabled(ConfigKind.CLIENT, clientId, "Client 不可用");
        if (!StringUtils.hasText(client.getRefId())) {
            throw new ChatException("Client 未绑定模型");
        }
        AiConfigRecord model = requireEnabled(ConfigKind.MODEL, client.getRefId(), "Model 不可用");
        if (!StringUtils.hasText(model.getName())) {
            throw new ChatException("Model 名称不能为空");
        }
        if (!StringUtils.hasText(model.getRefId())) {
            throw new ChatException("Model 未绑定 API");
        }
        AiConfigRecord api = requireEnabled(ConfigKind.API, model.getRefId(), "API 不可用");
        if (!StringUtils.hasText(api.getContent())) {
            throw new ChatException("API 地址不能为空");
        }
        if (!StringUtils.hasText(api.getSecret())) {
            throw new ChatException("API Key 不能为空");
        }

        OpenAiApi openAiApi = OpenAiApi.builder()
                .baseUrl(api.getContent())
                .apiKey(api.getSecret())
                .build();
        OpenAiChatModel chatModel = OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(OpenAiChatOptions.builder().model(model.getName()).build())
                .build();
        return ChatClient.builder(chatModel)
                .defaultSystem(systemPromptForClient(clientId))
                .build();
    }

    String systemPromptForClient(String clientId) {
        String system = aiConfigRepository.list(ConfigKind.CONFIG).stream()
                .filter(this::enabled)
                .filter(record -> "client".equals(record.getOwnerType()))
                .filter(record -> clientId.equals(record.getContent()))
                .filter(record -> "prompt".equals(record.getConfigType()))
                .filter(record -> StringUtils.hasText(record.getRefId()))
                .map(binding -> aiConfigRepository.find(ConfigKind.PROMPT, binding.getRefId()))
                .filter(this::enabled)
                .map(AiConfigRecord::getContent)
                .filter(StringUtils::hasText)
                .reduce("", String::concat);
        return StringUtils.hasText(system) ? system : DEFAULT_SYSTEM;
    }

    private AiConfigRecord requireEnabled(ConfigKind kind, String configId, String message) {
        AiConfigRecord record = aiConfigRepository.find(kind, configId);
        if (!enabled(record)) {
            throw new ChatException(message);
        }
        return record;
    }

    private boolean enabled(AiConfigRecord record) {
        return record != null && record.getStatus() != null && record.getStatus() == ENABLED;
    }
}
