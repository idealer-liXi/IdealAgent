package com.idealagent.domain.ai.service.work.step;

import com.idealagent.domain.ai.model.entity.ExecuteResponseEntity;
import com.idealagent.domain.ai.model.vo.AiFlowVO;
import com.idealagent.domain.ai.service.armory.IChatClientArmory;
import com.idealagent.domain.ai.service.work.ExecuteContext;
import com.idealagent.domain.ai.service.work.WorkChatGateway;
import com.idealagent.domain.ai.service.work.WorkEventSink;
import com.idealagent.domain.ai.service.work.WorkException;
import com.idealagent.domain.ai.service.work.WorkJsonParser;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.util.StringUtils;

abstract class StepNodeSupport {
    protected final IChatClientArmory armory;
    protected final WorkChatGateway chatGateway;
    protected final WorkJsonParser parser;

    protected StepNodeSupport(IChatClientArmory armory, WorkChatGateway chatGateway, WorkJsonParser parser) {
        this.armory = armory;
        this.chatGateway = chatGateway;
        this.parser = parser;
    }

    protected AiFlowVO flow(ExecuteContext context, String role) {
        AiFlowVO flow = context.getFlowMap().get(role);
        if (flow == null || !StringUtils.hasText(flow.getClientId()) || !StringUtils.hasText(flow.getUserPrompt())) {
            throw new WorkException("Work Flow 配置不完整");
        }
        return flow;
    }

    protected String call(AiFlowVO flow, String prompt) {
        ChatClient client = armory.resolve(flow.getClientId());
        return chatGateway.complete(client, prompt);
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
