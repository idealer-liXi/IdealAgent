package com.idealagent.domain.ai.model.entity;

public class ExecuteResponseEntity {
    private String clientType;
    private String sectionType;
    private String sectionContent;
    private Integer round;
    private Integer pace;
    private Integer step;
    private String sessionId;
    private Long timestamp;

    public static ExecuteResponseEntity section(String sectionType, String sectionContent, Integer step, String sessionId) {
        return createResponse(null, sectionType, sectionContent, null, null, step, sessionId);
    }

    public static ExecuteResponseEntity createAnalyzerResponse(String sectionType, String sectionContent, Integer round, String sessionId) {
        return createResponse("analyzer", sectionType, sectionContent, round, null, null, sessionId);
    }

    public static ExecuteResponseEntity createPerformerResponse(String sectionType, String sectionContent, Integer round, String sessionId) {
        return createResponse("performer", sectionType, sectionContent, round, null, null, sessionId);
    }

    public static ExecuteResponseEntity createSupervisorResponse(String sectionType, String sectionContent, Integer round, String sessionId) {
        return createResponse("supervisor", sectionType, sectionContent, round, null, null, sessionId);
    }

    public static ExecuteResponseEntity createSummarizerResponse(String sectionType, String sectionContent, Integer round, String sessionId) {
        return createResponse("summarizer", sectionType, sectionContent, round, null, null, sessionId);
    }

    public static ExecuteResponseEntity createInspectorResponse(String sectionType, String sectionContent, String sessionId) {
        return createResponse("inspector", sectionType, sectionContent, null, null, null, sessionId);
    }

    public static ExecuteResponseEntity createPlannerResponse(String sectionType, String sectionContent, String sessionId) {
        return createResponse("planner", sectionType, sectionContent, null, null, null, sessionId);
    }

    public static ExecuteResponseEntity createRunnerResponse(String sectionType, String sectionContent, Integer step, String sessionId) {
        return createResponse("runner", sectionType, sectionContent, null, null, step, sessionId);
    }

    public static ExecuteResponseEntity createReplierResponse(String sectionType, String sectionContent, String sessionId) {
        return createResponse("replier", sectionType, sectionContent, null, null, null, sessionId);
    }

    public static ExecuteResponseEntity createObserverResponse(String sectionType, String sectionContent, Integer pace, String sessionId) {
        return createResponse("observer", sectionType, sectionContent, null, pace, null, sessionId);
    }

    public static ExecuteResponseEntity createReasonerResponse(String sectionType, String sectionContent, Integer pace, String sessionId) {
        return createResponse("reasoner", sectionType, sectionContent, null, pace, null, sessionId);
    }

    public static ExecuteResponseEntity createActorResponse(String sectionType, String sectionContent, Integer pace, String sessionId) {
        return createResponse("actor", sectionType, sectionContent, null, pace, null, sessionId);
    }

    public static ExecuteResponseEntity createEvaluatorResponse(String sectionType, String sectionContent, Integer pace, String sessionId) {
        return createResponse("evaluator", sectionType, sectionContent, null, pace, null, sessionId);
    }

    public static ExecuteResponseEntity createCompleteResponse(String sectionContent, String sessionId) {
        return createResponse("complete", null, sectionContent, null, null, null, sessionId);
    }

    public static ExecuteResponseEntity createResponse(String clientType, String sectionType, String sectionContent, Integer round, Integer pace, Integer step, String sessionId) {
        ExecuteResponseEntity response = new ExecuteResponseEntity();
        response.setClientType(clientType);
        response.setSectionType(sectionType);
        response.setSectionContent(sectionContent);
        response.setRound(round);
        response.setPace(pace);
        response.setStep(step);
        response.setSessionId(sessionId);
        response.setTimestamp(System.currentTimeMillis());
        return response;
    }

    public static ExecuteResponseEntity complete(String sessionId) {
        return createCompleteResponse("执行完成", sessionId);
    }

    public String getClientType() {
        return clientType;
    }

    public void setClientType(String clientType) {
        this.clientType = clientType;
    }

    public String getSectionType() {
        return sectionType;
    }

    public void setSectionType(String sectionType) {
        this.sectionType = sectionType;
    }

    public String getSectionContent() {
        return sectionContent;
    }

    public void setSectionContent(String sectionContent) {
        this.sectionContent = sectionContent;
    }

    public Integer getStep() {
        return step;
    }

    public Integer getRound() {
        return round;
    }

    public void setRound(Integer round) {
        this.round = round;
    }

    public Integer getPace() {
        return pace;
    }

    public void setPace(Integer pace) {
        this.pace = pace;
    }

    public void setStep(Integer step) {
        this.step = step;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }
}
