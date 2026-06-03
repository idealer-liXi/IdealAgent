package com.idealagent.domain.ai.service.dispatch;

import com.idealagent.domain.ai.service.armory.IChatClientArmory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class ChatDispatchService implements IChatDispatchService {
    private final IChatClientArmory armory;

    public ChatDispatchService(IChatClientArmory armory) {
        this.armory = armory;
    }

    @Override
    public ChatClient dispatchChatClient(String clientId) {
        return armory.resolve(clientId);
    }
}
