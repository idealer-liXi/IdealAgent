package com.idealagent.domain.ai.model.vo;

public class WorkAgentOptionVO {
    private String agentId;
    private String agentName;
    private String agentType;
    private String agentDesc;

    public WorkAgentOptionVO() {
    }

    public WorkAgentOptionVO(String agentId, String agentName, String agentType, String agentDesc) {
        this.agentId = agentId;
        this.agentName = agentName;
        this.agentType = agentType;
        this.agentDesc = agentDesc;
    }

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
}
