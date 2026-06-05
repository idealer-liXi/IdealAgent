package com.idealagent.domain.ai.service.work.step;

import com.fasterxml.jackson.databind.JsonNode;
import com.idealagent.domain.ai.model.entity.ExecuteRequestEntity;
import com.idealagent.domain.ai.model.vo.AiFlowVO;
import com.idealagent.domain.ai.service.armory.IChatClientArmory;
import com.idealagent.domain.ai.service.work.ExecuteContext;
import com.idealagent.domain.ai.service.work.WorkChatGateway;
import com.idealagent.domain.ai.service.work.WorkEventSink;
import com.idealagent.domain.ai.service.work.WorkJsonParser;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

import static com.idealagent.domain.ai.service.work.step.StepConstants.PLANNER;
import static com.idealagent.domain.ai.service.work.step.StepConstants.PLANNER_STEP;

@Service
public class StepPlannerNode extends StepNodeSupport {
    public StepPlannerNode(IChatClientArmory armory, WorkChatGateway chatGateway, WorkJsonParser parser) {
        super(armory, chatGateway, parser);
    }

    public void execute(ExecuteRequestEntity request, ExecuteContext context, WorkEventSink sink) {
        try {
            AiFlowVO flow = flow(context, PLANNER);
            String inspectorResponse = context.getValue("inspector_response");
            JsonNode result = parser.parseArray(call(flow, flow.getUserPrompt().formatted(request.getUserMessage(), inspectorResponse)));
            List<String> steps = new ArrayList<>();
            for (JsonNode item : result) {
                String content = item.toString();
                steps.add(content);
                emit(sink, PLANNER_STEP, content, null, request.getSessionId());
            }
            context.setValue("planner_response", steps);
        } catch (Exception e) {
            context.setValue("planner_response", List.of());
            emitException(sink, PLANNER, e, null, request.getSessionId());
        }
    }
}
