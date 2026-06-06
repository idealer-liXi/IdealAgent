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
import com.idealagent.domain.ai.service.work.WorkJsonParser;
import org.springframework.stereotype.Service;

import static com.idealagent.domain.ai.service.work.step.StepConstants.INSPECTOR;
import static com.idealagent.domain.ai.service.work.step.StepConstants.INSPECTOR_MCP;

@Service
public class StepInspectorNode extends StepNodeSupport {
    public StepInspectorNode(IChatClientArmory armory, IMcpToolService mcpToolService, WorkChatGateway chatGateway, WorkJsonParser parser, RuntimeMessageBuilder messageBuilder) {
        super(armory, mcpToolService, chatGateway, parser, messageBuilder);
    }

    public void execute(ExecuteRequestEntity request, ExecuteContext context, WorkEventSink sink) {
        try {
            AiFlowVO flow = flow(context, INSPECTOR);
            JsonNode result = parser.parseArray(call(flow, flow.getUserPrompt().formatted(request.getUserMessage()), request));
            context.setValue("inspector_response", result.toString());
            for (JsonNode item : result) {
                emit(sink, INSPECTOR_MCP, item.toString(), null, request.getSessionId());
            }
        } catch (Exception e) {
            context.setValue("inspector_response", "[]");
            emitException(sink, INSPECTOR, e, null, request.getSessionId());
        }
    }
}
