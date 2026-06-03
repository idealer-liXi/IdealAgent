package com.idealagent.domain.session.model.vo;

import java.time.LocalDateTime;

public record ChatMessageVO(String messageId, String sessionId, String role, String content, LocalDateTime createTime) {
}
