package com.idealagent.domain.ai.service.armory;

import org.springframework.ai.chat.client.ChatClient;

public interface IChatClientArmory {
    ChatClient resolve(String clientId);
}
