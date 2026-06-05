package com.idealagent.domain.ai.model.entity;

public class WorkAgent {
    private String agentId;
    private String agentName;
    private String agentType;
    private String agentDesc;
    private Integer status;

    public String getAgentId() {
        return agentId;
    }

    public void setAgentId(String agentId) {
        this.agentId = agentId;
    }

    public String getAgentName() {
        return agentName;
    }

    public void setAgentName(String agentName) {
        this.agentName = agentName;
    }

    public String getAgentType() {
        return agentType;
    }

    public void setAgentType(String agentType) {
        this.agentType = agentType;
    }

    public String getAgentDesc() {
        return agentDesc;
    }

    public void setAgentDesc(String agentDesc) {
        this.agentDesc = agentDesc;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}
