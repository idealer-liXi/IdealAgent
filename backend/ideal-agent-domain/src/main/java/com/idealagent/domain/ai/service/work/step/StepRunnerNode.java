package com.idealagent.domain.ai.service.work.step;

import com.fasterxml.jackson.databind.JsonNode;
import com.idealagent.domain.ai.model.entity.ExecuteRequestEntity;
import com.idealagent.domain.ai.model.vo.AiFlowVO;
import com.idealagent.domain.ai.service.armory.IChatClientArmory;
import com.idealagent.domain.ai.service.augment.IMcpToolService;
import com.idealagent.domain.ai.service.chat.RuntimeMessageBuilder;
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
    public StepRunnerNode(IChatClientArmory armory, IMcpToolService mcpToolService, WorkChatGateway chatGateway, WorkJsonParser parser, RuntimeMessageBuilder messageBuilder) {
        super(armory, mcpToolService, chatGateway, parser, messageBuilder);
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
            String response;
            String stepPrompt = enrichPlannerStep(context, plannerStep);
            String prompt = flow.getUserPrompt().formatted(request.getUserMessage(), inspectorResponse, stepPrompt);
            try {
                response = requiresMcpTools(plannerStep) ? callWithToolsNamedIn(flow, prompt, request, plannerStep) : callWithoutTools(flow, prompt, request);
            } catch (Exception e) {
                emitException(sink, RUNNER, e, step, request.getSessionId());
                return;
            }
            try {
                JsonNode result = parser.parseObject(response);
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

    private String enrichPlannerStep(ExecuteContext context, String plannerStep) {
        String history = context.getExecutionHistory().isEmpty() ? "[暂无已完成步骤]" : context.getExecutionHistory().toString();
        return """
                【当前执行步骤】
                %s
                【已完成步骤执行结果】
                %s
                【执行边界】
                只执行当前执行步骤，不执行、不评价、不汇报后续步骤；如果当前步骤依赖前序结果，必须使用已完成步骤执行结果作为输入。
                """.formatted(plannerStep, history);
    }

    private boolean requiresMcpTools(String plannerStep) {
        try {
            JsonNode step = parser.parseObject(plannerStep);
            JsonNode mcp = step.get("step_mcp");
            if (mcp == null || mcp.isNull()) {
                return true;
            }
            String value = mcp.asText("").trim().toLowerCase();
            if (value.isBlank()) {
                return false;
            }
            return !(value.contains("无需工具")
                    || value.contains("不需要工具")
                    || value.contains("无需调用")
                    || value.contains("no tool")
                    || value.equals("none")
                    || value.equals("null")
                    || value.equals("无"));
        } catch (Exception ignored) {
            return true;
        }
    }
}
