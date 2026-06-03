package com.idealagent.domain.ai.model.vo;

import com.idealagent.domain.session.model.vo.ChatMessageVO;

public record ChatResponseVO(String sessionId, ChatMessageVO assistantMessage) {
}
