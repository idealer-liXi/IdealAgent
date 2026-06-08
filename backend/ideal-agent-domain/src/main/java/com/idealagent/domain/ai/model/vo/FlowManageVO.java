package com.idealagent.domain.ai.model.vo;

public record FlowManageVO(
        String agentId,
        String clientId,
        String clientRole,
        String userPrompt,
        Integer flowSeq) {
}
