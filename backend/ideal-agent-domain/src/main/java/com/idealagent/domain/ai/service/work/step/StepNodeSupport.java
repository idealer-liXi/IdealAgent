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
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

abstract class StepNodeSupport {
    private static final Pattern TOOL_FIELD_PATTERN = Pattern.compile("\\\"tool\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"");

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
            return chatGateway.complete(client, messageBuilder.build(request.getUserId(), request.getSessionId(), flow.getClientId(), prompt, null, "work"), tools.toolCallbackProvider());
        }
    }

    protected String callWithoutTools(AiFlowVO flow, String prompt, ExecuteRequestEntity request) {
        ChatClient client = armory.resolve(flow.getClientId());
        return chatGateway.complete(client, messageBuilder.build(request.getUserId(), request.getSessionId(), flow.getClientId(), prompt, null, "work"), null);
    }

    protected String callWithToolsNamedIn(AiFlowVO flow, String prompt, ExecuteRequestEntity request, String toolPlan) {
        ChatClient client = armory.resolve(flow.getClientId());
        try (McpToolHandle tools = mcpToolService.augmentMcpTool(request.getUserId(), flow.getClientId())) {
            ToolCallbackProvider provider = filterTools(tools.toolCallbackProvider(), toolPlan);
            if (provider == null) {
                throw new WorkException("MCP 工具未匹配: " + toolNameHint(toolPlan));
            }
            return chatGateway.complete(client, messageBuilder.build(request.getUserId(), request.getSessionId(), flow.getClientId(), prompt, null, "work"), provider);
        }
    }

    private ToolCallbackProvider filterTools(ToolCallbackProvider provider, String toolPlan) {
        if (provider == null || !StringUtils.hasText(toolPlan)) {
            return null;
        }
        List<ToolCallback> callbacks = Arrays.stream(provider.getToolCallbacks())
                .filter(callback -> callback.getToolDefinition() != null)
                .filter(callback -> matchesToolName(toolPlan, callback.getToolDefinition().name()))
                .toList();
        if (!callbacks.isEmpty()) {
            return ToolCallbackProvider.from(callbacks);
        }
        return hasExplicitToolRequest(toolPlan) ? null : provider;
    }

    private boolean matchesToolName(String toolPlan, String callbackName) {
        if (!StringUtils.hasText(callbackName)) {
            return false;
        }
        if (toolPlan.contains(callbackName)) {
            return true;
        }
        int index = callbackName.lastIndexOf('_');
        String shortName = index >= 0 ? callbackName.substring(index + 1) : callbackName;
        return StringUtils.hasText(shortName) && toolPlan.contains(shortName);
    }

    private String toolNameHint(String toolPlan) {
        String normalized = normalizeToolPlan(toolPlan);
        Matcher matcher = TOOL_FIELD_PATTERN.matcher(normalized);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return StringUtils.hasText(toolPlan) ? normalized : "unknown";
    }

    private boolean hasExplicitToolRequest(String toolPlan) {
        return TOOL_FIELD_PATTERN.matcher(normalizeToolPlan(toolPlan)).find();
    }

    private String normalizeToolPlan(String toolPlan) {
        return toolPlan == null ? "" : toolPlan.replace("\\\"", "\"");
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
