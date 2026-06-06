package com.idealagent.domain.ai.service.work;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class WorkChatGateway {
    public String complete(ChatClient client, String prompt) {
        return complete(client, prompt, null);
    }

    public String complete(ChatClient client, String prompt, ToolCallbackProvider toolCallbackProvider) {
        ChatClient.ChatClientRequestSpec request = client.prompt(prompt);
        return complete(request, toolCallbackProvider);
    }

    public String complete(ChatClient client, List<Message> messages, ToolCallbackProvider toolCallbackProvider) {
        ChatClient.ChatClientRequestSpec request = client.prompt().messages(messages);
        return complete(request, toolCallbackProvider);
    }

    private String complete(ChatClient.ChatClientRequestSpec request, ToolCallbackProvider toolCallbackProvider) {
        if (toolCallbackProvider != null) {
            request = request.toolCallbacks(toolCallbackProvider);
        }
        String content = request.call().content();
        if (content == null || content.isBlank()) {
            throw new WorkException("模型返回内容为空");
        }
        return content;
    }
}
