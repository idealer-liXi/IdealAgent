package com.idealagent.infrastructure.persistent.po;

public class AiFlow {
    private String agentId;
    private String clientId;
    private String clientRole;
    private String userPrompt;
    private Integer flowSeq;

    public String getAgentId() {
        return agentId;
    }

    public void setAgentId(String agentId) {
        this.agentId = agentId;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientRole() {
        return clientRole;
    }

    public void setClientRole(String clientRole) {
        this.clientRole = clientRole;
    }

    public String getUserPrompt() {
        return userPrompt;
    }

    public void setUserPrompt(String userPrompt) {
        this.userPrompt = userPrompt;
    }

    public Integer getFlowSeq() {
        return flowSeq;
    }

    public void setFlowSeq(Integer flowSeq) {
        this.flowSeq = flowSeq;
    }
}
