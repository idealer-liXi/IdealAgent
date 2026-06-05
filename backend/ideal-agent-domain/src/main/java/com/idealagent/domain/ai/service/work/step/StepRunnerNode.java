package com.idealagent.domain.ai.service.work.step;

import com.fasterxml.jackson.databind.JsonNode;
import com.idealagent.domain.ai.model.entity.ExecuteRequestEntity;
import com.idealagent.domain.ai.model.vo.AiFlowVO;
import com.idealagent.domain.ai.service.armory.IChatClientArmory;
import com.idealagent.domain.ai.service.work.ExecuteContext;
import com.idealagent.domain.ai.service.work.WorkChatGateway;
import com.idealagent.domain.ai.service.work.WorkEventSink;
import com.idealagent.domain.ai.service.work.WorkException;
import com.idealagent.domain.ai.service.work.WorkJsonParser;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.idealagent.domain.ai.service.work.step.StepConstants.RUNNER;
import static com.idealagent.domain.ai.service.work.step.StepConstants.RUNNER_RESULT;
import static com.idealagent.domain.ai.service.work.step.StepConstants.RUNNER_STATUS;

@Service
public class StepRunnerNode extends StepNodeSupport {
    public StepRunnerNode(IChatClientArmory armory, WorkChatGateway chatGateway, WorkJsonParser parser) {
        super(armory, chatGateway, parser);
    }

    public void execute(ExecuteRequestEntity request, ExecuteContext context, WorkEventSink sink) {
        List<String> plannerSteps = context.getValue("planner_response");
        if (plannerSteps == null || plannerSteps.isEmpty()) {
            return;
        }
        AiFlowVO flow = flow(context, RUNNER);
        String inspectorResponse = context.getValue("inspector_response");
        int maxRetry = request.getMaxRetry() == null ? 2 : request.getMaxRetry();
        for (int i = 0; i < plannerSteps.size(); i++) {
            executeStep(request, context, sink, flow, inspectorResponse, plannerSteps.get(i), i + 1, maxRetry);
        }
    }

    private void executeStep(ExecuteRequestEntity request, ExecuteContext context, WorkEventSink sink, AiFlowVO flow, String inspectorResponse, String plannerStep, int step, int maxRetry) {
        Exception lastError = null;
        for (int attempt = 1; attempt <= maxRetry; attempt++) {
            try {
                String prompt = flow.getUserPrompt().formatted(request.getUserMessage(), inspectorResponse, plannerStep);
                JsonNode result = parser.parseObject(call(flow, prompt));
                String status = text(result, RUNNER_STATUS);
                String runnerResult = text(result, RUNNER_RESULT);
                if ("FAIL".equalsIgnoreCase(status)) {
                    throw new WorkException(runnerResult == null || runnerResult.isBlank() ? "客户端执行失败" : runnerResult);
                }
                emit(sink, RUNNER_RESULT, runnerResult, step, request.getSessionId());
                emit(sink, RUNNER_STATUS, status, step, request.getSessionId());
                context.getExecutionHistory().append("【任务运行专家】\n").append(result).append('\n');
                return;
            } catch (Exception e) {
                lastError = e;
            }
        }
        emitException(sink, RUNNER, lastError == null ? new WorkException("执行异常") : lastError, step, request.getSessionId());
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }
}
