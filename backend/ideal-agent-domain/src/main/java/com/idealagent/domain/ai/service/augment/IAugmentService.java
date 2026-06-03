package com.idealagent.domain.ai.service.augment;

import org.springframework.ai.chat.messages.Message;

import java.util.List;

public interface IAugmentService {
    List<Message> augmentRagMessage(Long userId, String userMessage, String ragTag);
}
