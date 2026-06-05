package com.idealagent.domain.ai.service.work.react;

import com.fasterxml.jackson.databind.JsonNode;
import com.idealagent.domain.ai.model.entity.ExecuteRequestEntity;
import com.idealagent.domain.ai.model.entity.ExecuteResponseEntity;
import com.idealagent.domain.ai.model.vo.AiFlowVO;
import com.idealagent.domain.ai.repository.IWorkAgentRepository;
import com.idealagent.domain.ai.service.armory.IChatClientArmory;
import com.idealagent.domain.ai.service.work.IExecuteStrategy;
import com.idealagent.domain.ai.service.work.WorkChatGateway;
import com.idealagent.domain.ai.service.work.WorkEventSink;
import com.idealagent.domain.ai.service.work.WorkException;
import com.idealagent.domain.ai.service.work.WorkJsonParser;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

@Service
public class ExecuteReactStrategy implements IExecuteStrategy {
    private static final List<String> REQUIRED = List.of("observer", "reasoner", "actor", "evaluator");

    private final IWorkAgentRepository repository;
    private final IChatClientArmory armory;
    private final WorkChatGateway chatGateway;
    private final WorkJsonParser parser;

    public ExecuteReactStrategy(IWorkAgentRepository repository, IChatClientArmory armory, WorkChatGateway chatGateway, WorkJsonParser parser) {
        this.repository = repository;
        this.armory = armory;
        this.chatGateway = chatGateway;
        this.parser = parser;
    }

    @Override
    public void execute(ExecuteRequestEntity request, WorkEventSink sink) {
        Map<String, AiFlowVO> flows = repository.listFlowMap(request.getAgentId());
        REQUIRED.forEach(role -> require(flows, role));
        int maxPace = request.getMaxPace() == null ? 3 : request.getMaxPace();
        int pace = 0;
        boolean completed = false;
        String currentTask = "这里是起点，请先观察当前任务状态，并判断是否需要继续执行下一小步。";
        StringBuilder history = new StringBuilder();

        while (pace <= maxPace && !completed) {
            JsonNode observer = call(flows.get("observer"), format(flows.get("observer").getUserPrompt(), pace, maxPace, request.getUserMessage(), currentTask, emptyHistory(history)), sink, "observer", request.getSessionId(), pace);
            completed = isCompleted(text(observer, "observer_status"));
            if (completed) {
                break;
            }
            JsonNode reasoner = call(flows.get("reasoner"), format(flows.get("reasoner").getUserPrompt(), request.getUserMessage(), observer.toString(), emptyHistory(history)), sink, "reasoner", request.getSessionId(), pace);
            currentTask = text(reasoner, "reasoner_action") == null ? reasoner.toString() : text(reasoner, "reasoner_action");
            JsonNode actor = call(flows.get("actor"), format(flows.get("actor").getUserPrompt(), request.getUserMessage(), observer.toString(), reasoner.toString()), sink, "actor", request.getSessionId(), pace);
            history.append("=== 第 ").append(pace).append(" 轮执行记录 ===\n")
                    .append("【任务观察专家】\n").append(observer).append('\n')
                    .append("【任务推理专家】\n").append(reasoner).append('\n')
                    .append("【任务行动专家】\n").append(actor).append('\n');
            currentTask = text(actor, "actor_result") == null ? actor.toString() : text(actor, "actor_result");
            pace++;
        }
        call(flows.get("evaluator"), format(flows.get("evaluator").getUserPrompt(), request.getUserMessage(), emptyHistory(history)), sink, "evaluator", request.getSessionId(), pace);
        sink.complete(ExecuteResponseEntity.createCompleteResponse("执行完成", request.getSessionId()));
    }

    @Override
    public String getType() {
        return "react";
    }

    private JsonNode call(AiFlowVO flow, String prompt, WorkEventSink sink, String role, String sessionId, Integer pace) {
        try {
            ChatClient client = armory.resolve(flow.getClientId());
            JsonNode node = parser.parseObject(chatGateway.complete(client, prompt));
            node.fields().forEachRemaining(entry -> sink.message(response(role, entry.getKey(), entry.getValue().asText(), sessionId, pace)));
            return node;
        } catch (Exception e) {
            ExecuteResponseEntity response = response(role, role + "_exception", e.getMessage() == null ? "执行异常" : e.getMessage(), sessionId, pace);
            sink.message(response);
            return parser.parseObject("{\"" + role + "_exception\":\"" + escape(response.getSectionContent()) + "\"}");
        }
    }

    private ExecuteResponseEntity response(String role, String sectionType, String content, String sessionId, Integer pace) {
        return switch (role) {
            case "observer" -> ExecuteResponseEntity.createObserverResponse(sectionType, content, pace, sessionId);
            case "reasoner" -> ExecuteResponseEntity.createReasonerResponse(sectionType, content, pace, sessionId);
            case "actor" -> ExecuteResponseEntity.createActorResponse(sectionType, content, pace, sessionId);
            case "evaluator" -> ExecuteResponseEntity.createEvaluatorResponse(sectionType, content, pace, sessionId);
            default -> ExecuteResponseEntity.createResponse(role, sectionType, content, null, pace, null, sessionId);
        };
    }

    private void require(Map<String, AiFlowVO> flows, String role) {
        AiFlowVO flow = flows == null ? null : flows.get(role);
        if (flow == null || !StringUtils.hasText(flow.getClientId()) || !StringUtils.hasText(flow.getUserPrompt())) {
            throw new WorkException("Work Flow 配置不完整");
        }
    }

    private String format(String prompt, Object... args) {
        try {
            return prompt.formatted(args);
        } catch (Exception e) {
            return prompt + " " + java.util.Arrays.toString(args);
        }
    }

    private String emptyHistory(StringBuilder history) {
        return history.isEmpty() ? "[暂无记录]" : history.toString();
    }

    private boolean isCompleted(String status) {
        return "COMPLETED".equalsIgnoreCase(status) || "DONE".equalsIgnoreCase(status) || "PASS".equalsIgnoreCase(status);
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    private String escape(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
