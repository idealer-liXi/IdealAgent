package com.idealagent.domain.session.model.vo;

import java.time.LocalDateTime;

public record ChatSessionVO(String sessionId, String title, String clientId, LocalDateTime createTime, LocalDateTime updateTime) {
}
