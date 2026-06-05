package com.idealagent.domain.ai.model.entity;

public class ExecuteRequestEntity {
    private String agentId;
    private String userMessage;
    private String sessionId;
    private Integer maxRound;
    private Integer maxRetry;
    private Integer maxPace;

    public String getAgentId() {
        return agentId;
    }

    public void setAgentId(String agentId) {
        this.agentId = agentId;
    }

    public String getUserMessage() {
        return userMessage;
    }

    public void setUserMessage(String userMessage) {
        this.userMessage = userMessage;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public Integer getMaxRound() {
        return maxRound;
    }

    public void setMaxRound(Integer maxRound) {
        this.maxRound = maxRound;
    }

    public Integer getMaxRetry() {
        return maxRetry;
    }

    public void setMaxRetry(Integer maxRetry) {
        this.maxRetry = maxRetry;
    }

    public Integer getMaxPace() {
        return maxPace;
    }

    public void setMaxPace(Integer maxPace) {
        this.maxPace = maxPace;
    }
}
