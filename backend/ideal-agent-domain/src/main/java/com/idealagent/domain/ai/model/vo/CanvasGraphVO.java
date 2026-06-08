package com.idealagent.domain.ai.model.vo;

import java.util.List;
import java.util.Map;

public record CanvasGraphVO(
        AgentManageVO agent,
        List<FlowManageVO> flows,
        List<Map<String, Object>> clients,
        List<Map<String, Object>> models,
        List<Map<String, Object>> apis,
        List<Map<String, Object>> prompts,
        List<Map<String, Object>> advisors,
        List<Map<String, Object>> mcps,
        List<Map<String, Object>> configs) {
}
