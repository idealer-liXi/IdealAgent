package com.idealagent.domain.ai.service.work.step;

import com.idealagent.domain.ai.model.entity.ExecuteRequestEntity;
import com.idealagent.domain.ai.model.entity.ExecuteResponseEntity;
import com.idealagent.domain.ai.model.vo.AiFlowVO;
import com.idealagent.domain.ai.repository.IWorkAgentRepository;
import com.idealagent.domain.ai.service.work.ExecuteContext;
import com.idealagent.domain.ai.service.work.IExecuteStrategy;
import com.idealagent.domain.ai.service.work.WorkEventSink;
import com.idealagent.domain.ai.service.work.WorkException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Map;

import static com.idealagent.domain.ai.service.work.step.StepConstants.INSPECTOR;
import static com.idealagent.domain.ai.service.work.step.StepConstants.PLANNER;
import static com.idealagent.domain.ai.service.work.step.StepConstants.REPLIER;
import static com.idealagent.domain.ai.service.work.step.StepConstants.RUNNER;

@Service
public class ExecuteStepStrategy implements IExecuteStrategy {
    private final IWorkAgentRepository repository;
    private final StepInspectorNode inspectorNode;
    private final StepPlannerNode plannerNode;
    private final StepRunnerNode runnerNode;
    private final StepReplierNode replierNode;

    public ExecuteStepStrategy(IWorkAgentRepository repository,
                               StepInspectorNode inspectorNode,
                               StepPlannerNode plannerNode,
                               StepRunnerNode runnerNode,
                               StepReplierNode replierNode) {
        this.repository = repository;
        this.inspectorNode = inspectorNode;
        this.plannerNode = plannerNode;
        this.runnerNode = runnerNode;
        this.replierNode = replierNode;
    }

    @Override
    public void execute(ExecuteRequestEntity request, WorkEventSink sink) {
        ExecuteContext context = new ExecuteContext();
        Map<String, AiFlowVO> flowMap = repository.listFlowMap(request.getAgentId());
        validateFlow(flowMap, INSPECTOR);
        validateFlow(flowMap, PLANNER);
        validateFlow(flowMap, RUNNER);
        validateFlow(flowMap, REPLIER);
        context.setFlowMap(flowMap);

        inspectorNode.execute(request, context, sink);
        plannerNode.execute(request, context, sink);
        runnerNode.execute(request, context, sink);
        replierNode.execute(request, context, sink);
        sink.complete(ExecuteResponseEntity.complete(request.getSessionId()));
    }

    @Override
    public String getType() {
        return "step";
    }

    private void validateFlow(Map<String, AiFlowVO> flowMap, String role) {
        AiFlowVO flow = flowMap == null ? null : flowMap.get(role);
        if (flow == null || !StringUtils.hasText(flow.getClientId()) || !StringUtils.hasText(flow.getUserPrompt())) {
            throw new WorkException("Work Flow 配置不完整");
        }
    }
}
