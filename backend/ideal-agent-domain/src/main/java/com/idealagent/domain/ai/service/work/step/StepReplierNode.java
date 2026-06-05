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

import static com.idealagent.domain.ai.service.work.step.StepConstants.REPLIER;
import static com.idealagent.domain.ai.service.work.step.StepConstants.REPLIER_OVERVIEW;

@Service
public class StepReplierNode extends StepNodeSupport {
    public StepReplierNode(IChatClientArmory armory, WorkChatGateway chatGateway, WorkJsonParser parser) {
        super(armory, chatGateway, parser);
    }

    public void execute(ExecuteRequestEntity request, ExecuteContext context, WorkEventSink sink) {
        try {
            AiFlowVO flow = flow(context, REPLIER);
            String history = context.getExecutionHistory().isEmpty() ? "[暂无记录]" : context.getExecutionHistory().toString();
            JsonNode result = parser.parseObject(call(flow, flow.getUserPrompt().formatted(request.getUserMessage(), history)));
            JsonNode overview = result.get(REPLIER_OVERVIEW);
            if (overview != null && !overview.isNull()) {
                emit(sink, REPLIER_OVERVIEW, overview.asText(), null, request.getSessionId());
            }
        } catch (Exception e) {
            emitException(sink, REPLIER, e, null, request.getSessionId());
        }
    }
}
