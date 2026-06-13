package com.idealagent.domain.ai.model.dto;

import java.util.List;

public record WorkshopAgentCreateDTO(
        String agentName,
        String agentDesc,
        String strategy,
        String modelId,
        List<String> mcpIdList) {
}
