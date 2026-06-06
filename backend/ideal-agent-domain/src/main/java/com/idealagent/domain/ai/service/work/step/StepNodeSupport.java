package com.idealagent.domain.ai.service.work.step;

import com.idealagent.domain.ai.model.entity.ExecuteResponseEntity;
import com.idealagent.domain.ai.model.entity.ExecuteRequestEntity;
import com.idealagent.domain.ai.model.vo.AiFlowVO;
import com.idealagent.domain.ai.service.armory.IChatClientArmory;
import com.idealagent.domain.ai.service.augment.IMcpToolService;
import com.idealagent.domain.ai.service.augment.McpToolHandle;
import com.idealagent.domain.ai.service.chat.RuntimeMessageBuilder;
import com.idealagent.domain.ai.service.work.ExecuteContext;
import com.idealagent.domain.ai.service.work.WorkChatGateway;
import com.idealagent.domain.ai.service.work.WorkEventSink;
import com.idealagent.domain.ai.service.work.WorkException;
import com.idealagent.domain.ai.service.work.WorkJsonParser;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.util.StringUtils;

abstract class StepNodeSupport {
    protected final IChatClientArmory armory;
    protected final IMcpToolService mcpToolService;
    protected final WorkChatGateway chatGateway;
    protected final WorkJsonParser parser;
    protected final RuntimeMessageBuilder messageBuilder;

    protected StepNodeSupport(IChatClientArmory armory, IMcpToolService mcpToolService, WorkChatGateway chatGateway, WorkJsonParser parser, RuntimeMessageBuilder messageBuilder) {
        this.armory = armory;
        this.mcpToolService = mcpToolService;
        this.chatGateway = chatGateway;
        this.parser = parser;
        this.messageBuilder = messageBuilder;
    }

    protected AiFlowVO flow(ExecuteContext context, String role) {
        AiFlowVO flow = context.getFlowMap().get(role);
        if (flow == null || !StringUtils.hasText(flow.getClientId()) || !StringUtils.hasText(flow.getUserPrompt())) {
            throw new WorkException("Work Flow 配置不完整");
        }
        return flow;
    }

    protected String call(AiFlowVO flow, String prompt, ExecuteRequestEntity request) {
        ChatClient client = armory.resolve(flow.getClientId());
        try (McpToolHandle tools = mcpToolService.augmentMcpTool(request.getUserId(), flow.getClientId())) {
            return chatGateway.complete(client, messageBuilder.build(request.getUserId(), request.getSessionId(), flow.getClientId(), prompt, request.getRagTag(), "work"), tools.toolCallbackProvider());
        }
    }

    protected void emit(WorkEventSink sink, String sectionType, String content, Integer step, String sessionId) {
        if (StringUtils.hasText(content)) {
            sink.message(ExecuteResponseEntity.createResponse(clientType(sectionType), sectionType, content, null, null, step, sessionId));
        }
    }

    protected void emitException(WorkEventSink sink, String role, Exception e, Integer step, String sessionId) {
        String message = e.getMessage() == null ? "执行异常" : e.getMessage();
        sink.message(ExecuteResponseEntity.createResponse(role, role + "_exception", message, null, null, step, sessionId));
    }

    private String clientType(String sectionType) {
        if (sectionType == null) {
            return null;
        }
        int index = sectionType.indexOf('_');
        return index > 0 ? sectionType.substring(0, index) : null;
    }
}
