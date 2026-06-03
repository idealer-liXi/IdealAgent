package com.idealagent.domain.ai.service.dispatch;

import org.springframework.ai.chat.client.ChatClient;

public interface IChatDispatchService {
    ChatClient dispatchChatClient(String clientId);
}
