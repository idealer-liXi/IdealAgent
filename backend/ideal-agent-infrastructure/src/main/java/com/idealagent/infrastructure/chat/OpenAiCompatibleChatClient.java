package com.idealagent.infrastructure.chat;

import com.idealagent.domain.chat.model.entity.ChatMessage;
import com.idealagent.domain.chat.service.ChatException;
import com.idealagent.domain.chat.service.IChatClient;
import com.idealagent.domain.config.model.entity.AiConfigRecord;
import com.idealagent.domain.config.repository.IAiConfigRepository;
import com.idealagent.domain.config.service.ConfigKind;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class OpenAiCompatibleChatClient implements IChatClient {
    private static final String LOCAL_ECHO_CLIENT = "local_echo";
    private static final int ENABLED = 1;
    private static final Pattern CONTENT_PATTERN = Pattern.compile("\\\"content\\\"\\s*:\\s*\\\"((?:\\\\.|[^\\\\\\\"])*)\\\"");

    private final IAiConfigRepository aiConfigRepository;
    private final OpenAiTransport transport;

    @Autowired
    public OpenAiCompatibleChatClient(IAiConfigRepository aiConfigRepository) {
        this(aiConfigRepository, new JavaHttpOpenAiTransport());
    }

    OpenAiCompatibleChatClient(IAiConfigRepository aiConfigRepository, OpenAiTransport transport) {
        this.aiConfigRepository = aiConfigRepository;
        this.transport = transport;
    }

    @Override
    public String complete(String clientId, List<ChatMessage> history) {
        if (!StringUtils.hasText(clientId) || LOCAL_ECHO_CLIENT.equals(clientId)) {
            return localEcho(clientId, history);
        }
        ClientConfig config = resolveConfig(clientId);
        OpenAiResponse response = transport.post(chatCompletionUrl(config.api().getContent()), headers(config.api()), requestBody(config.model(), history));
        if (response == null) {
            throw new ChatException("模型调用失败：响应为空");
        }
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new ChatException("模型调用失败：" + response.body());
        }
        return assistantContent(response.body());
    }

    private ClientConfig resolveConfig(String clientId) {
        AiConfigRecord client = enabledRecord(ConfigKind.CLIENT, clientId, "Client 不存在或未启用");
        if (!StringUtils.hasText(client.getRefId())) {
            throw new ChatException("Client 未绑定模型");
        }
        AiConfigRecord model = enabledRecord(ConfigKind.MODEL, client.getRefId(), "模型不存在或未启用");
        if (!StringUtils.hasText(model.getName())) {
            throw new ChatException("模型名称不能为空");
        }
        if (!StringUtils.hasText(model.getRefId())) {
            throw new ChatException("模型未绑定 API");
        }
        AiConfigRecord api = enabledRecord(ConfigKind.API, model.getRefId(), "API 不存在或未启用");
        if (!StringUtils.hasText(api.getContent())) {
            throw new ChatException("API 地址不能为空");
        }
        if (!StringUtils.hasText(api.getSecret())) {
            throw new ChatException("API Key 不能为空");
        }
        return new ClientConfig(client, model, api);
    }

    private AiConfigRecord enabledRecord(ConfigKind kind, String configId, String message) {
        return aiConfigRepository.list(kind).stream()
                .filter(record -> configId.equals(record.getConfigId()))
                .filter(record -> record.getStatus() != null && record.getStatus() == ENABLED)
                .findFirst()
                .orElseThrow(() -> new ChatException(message));
    }

    private Map<String, String> headers(AiConfigRecord api) {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Authorization", "Bearer " + api.getSecret());
        headers.put("Content-Type", "application/json");
        return headers;
    }

    private String requestBody(AiConfigRecord model, List<ChatMessage> history) {
        List<String> messages = new ArrayList<>();
        for (ChatMessage item : history) {
            if (!StringUtils.hasText(item.getRole()) || !StringUtils.hasText(item.getContent())) {
                continue;
            }
            messages.add("{\"role\":\"" + jsonEscape(item.getRole()) + "\",\"content\":\"" + jsonEscape(item.getContent()) + "\"}");
        }
        return "{\"model\":\"" + jsonEscape(model.getName()) + "\",\"stream\":false,\"messages\":[" + String.join(",", messages) + "]}";
    }

    private String assistantContent(String body) {
        Matcher matcher = CONTENT_PATTERN.matcher(body == null ? "" : body);
        if (!matcher.find()) {
            throw new ChatException("模型返回内容为空");
        }
        String content = jsonUnescape(matcher.group(1));
        if (!StringUtils.hasText(content)) {
            throw new ChatException("模型返回内容为空");
        }
        return content;
    }

    private String jsonEscape(String value) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            switch (ch) {
                case '\\' -> builder.append("\\\\");
                case '"' -> builder.append("\\\"");
                case '\n' -> builder.append("\\n");
                case '\r' -> builder.append("\\r");
                case '\t' -> builder.append("\\t");
                default -> builder.append(ch);
            }
        }
        return builder.toString();
    }

    private String jsonUnescape(String value) {
        StringBuilder builder = new StringBuilder();
        boolean escaped = false;
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (!escaped) {
                if (ch == '\\') {
                    escaped = true;
                } else {
                    builder.append(ch);
                }
                continue;
            }
            switch (ch) {
                case 'n' -> builder.append('\n');
                case 'r' -> builder.append('\r');
                case 't' -> builder.append('\t');
                case '"' -> builder.append('"');
                case '\\' -> builder.append('\\');
                default -> builder.append(ch);
            }
            escaped = false;
        }
        if (escaped) {
            builder.append('\\');
        }
        return builder.toString();
    }

    private String chatCompletionUrl(String baseUrl) {
        String normalized = baseUrl.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized.endsWith("/v1") ? normalized + "/chat/completions" : normalized + "/v1/chat/completions";
    }

    private String localEcho(String clientId, List<ChatMessage> history) {
        ChatMessage latest = history.get(history.size() - 1);
        String resolvedClientId = StringUtils.hasText(clientId) ? clientId : LOCAL_ECHO_CLIENT;
        return "IdealAgent local chat client [" + resolvedClientId + "] received: " + latest.getContent();
    }

    record OpenAiResponse(int statusCode, String body) {
    }

    interface OpenAiTransport {
        OpenAiResponse post(String url, Map<String, String> headers, String body);
    }

    private record ClientConfig(AiConfigRecord client, AiConfigRecord model, AiConfigRecord api) {
    }

    private static class JavaHttpOpenAiTransport implements OpenAiTransport {
        private final HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        @Override
        public OpenAiResponse post(String url, Map<String, String> headers, String body) {
            try {
                HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(url))
                        .timeout(Duration.ofSeconds(60))
                        .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));
                headers.forEach(request::header);
                HttpResponse<String> response = httpClient.send(request.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                return new OpenAiResponse(response.statusCode(), response.body());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new ChatException("模型调用被中断");
            } catch (IOException | IllegalArgumentException e) {
                throw new ChatException("模型调用失败：" + e.getMessage());
            }
        }
    }
}
