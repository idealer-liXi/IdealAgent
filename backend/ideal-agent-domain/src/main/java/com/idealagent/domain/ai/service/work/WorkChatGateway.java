package com.idealagent.domain.ai.service.work;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class WorkChatGateway {
    public String complete(ChatClient client, String prompt) {
        return client.prompt(prompt).call().content();
    }
}
