package com.idealagent.domain.ai.model.vo;

import java.util.List;

public record FlowOptionsVO(
        List<AiConfigRecordVO> clients,
        List<AiConfigRecordVO> prompts,
        List<AiConfigRecordVO> mcps) {
}
