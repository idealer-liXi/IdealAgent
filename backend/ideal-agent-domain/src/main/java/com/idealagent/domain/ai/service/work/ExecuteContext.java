package com.idealagent.domain.ai.service.work;

import com.idealagent.domain.ai.model.vo.AiFlowVO;

import java.util.HashMap;
import java.util.Map;

public class ExecuteContext {
    private Map<String, AiFlowVO> flowMap = Map.of();
    private StringBuilder executionHistory = new StringBuilder();
    private final Map<String, Object> values = new HashMap<>();

    public Map<String, AiFlowVO> getFlowMap() {
        return flowMap;
    }

    public void setFlowMap(Map<String, AiFlowVO> flowMap) {
        this.flowMap = flowMap;
    }

    public StringBuilder getExecutionHistory() {
        return executionHistory;
    }

    public void setExecutionHistory(StringBuilder executionHistory) {
        this.executionHistory = executionHistory;
    }

    public <T> void setValue(String key, T value) {
        values.put(key, value);
    }

    @SuppressWarnings("unchecked")
    public <T> T getValue(String key) {
        return (T) values.get(key);
    }
}
