package com.idealagent.domain.ai.service.work.loop;

import com.fasterxml.jackson.databind.JsonNode;
import com.idealagent.domain.ai.model.entity.ExecuteRequestEntity;
import com.idealagent.domain.ai.model.entity.ExecuteResponseEntity;
import com.idealagent.domain.ai.model.vo.AiFlowVO;
import com.idealagent.domain.ai.repository.IWorkAgentRepository;
import com.idealagent.domain.ai.service.armory.IChatClientArmory;
import com.idealagent.domain.ai.service.augment.IMcpToolService;
import com.idealagent.domain.ai.service.augment.McpToolHandle;
import com.idealagent.domain.ai.service.chat.RuntimeMessageBuilder;
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
public class ExecuteLoopStrategy implements IExecuteStrategy {
    private static final List<String> REQUIRED = List.of("analyzer", "performer", "supervisor", "summarizer");

    private final IWorkAgentRepository repository;
    private final IChatClientArmory armory;
    private final IMcpToolService mcpToolService;
    private final WorkChatGateway chatGateway;
    private final WorkJsonParser parser;
    private final RuntimeMessageBuilder messageBuilder;

    public ExecuteLoopStrategy(IWorkAgentRepository repository, IChatClientArmory armory, IMcpToolService mcpToolService, WorkChatGateway chatGateway, WorkJsonParser parser, RuntimeMessageBuilder messageBuilder) {
        this.repository = repository;
        this.armory = armory;
        this.mcpToolService = mcpToolService;
        this.chatGateway = chatGateway;
        this.parser = parser;
        this.messageBuilder = messageBuilder;
    }

    @Override
    public void execute(ExecuteRequestEntity request, WorkEventSink sink) {
        Map<String, AiFlowVO> flows = repository.listFlowMap(request.getAgentId());
        REQUIRED.forEach(role -> require(flows, role));
        int maxRound = request.getMaxRound() == null ? 3 : request.getMaxRound();
        int round = 1;
        boolean completed = false;
        String currentTask = null;
        StringBuilder history = new StringBuilder();

        while (round <= maxRound && !completed) {
            JsonNode analyzer = call(flows.get("analyzer"), format(flows.get("analyzer").getUserPrompt(), round, maxRound, request.getUserMessage(), currentTask, emptyHistory(history)), sink, "analyzer", request, round, null);
            completed = isCompleted(text(analyzer, "analyzer_status")) || "100".equals(text(analyzer, "analyzer_progress"));
            if (completed) {
                break;
            }
            JsonNode performer = call(flows.get("performer"), format(flows.get("performer").getUserPrompt(), request.getUserMessage(), analyzer.toString()), sink, "performer", request, round, null);
            JsonNode supervisor = call(flows.get("supervisor"), format(flows.get("supervisor").getUserPrompt(), request.getUserMessage(), analyzer.toString(), performer.toString()), sink, "supervisor", request, round, null);
            String supervisorStatus = text(supervisor, "supervisor_status");
            completed = "PASS".equalsIgnoreCase(supervisorStatus);
            currentTask = "OPTIMIZE".equalsIgnoreCase(supervisorStatus) ? "根据任务监督专家的输出优化执行任务" : "根据任务监督专家的输出重新执行任务";
            history.append("=== 第 ").append(round).append(" 轮执行记录 ===\n")
                    .append("【任务分析专家】\n").append(analyzer).append('\n')
                    .append("【任务执行专家】\n").append(performer).append('\n')
                    .append("【任务监督专家】\n").append(supervisor).append('\n');
            round++;
        }
        call(flows.get("summarizer"), format(flows.get("summarizer").getUserPrompt(), request.getUserMessage(), emptyHistory(history)), sink, "summarizer", request, round, null);
        sink.complete(ExecuteResponseEntity.createCompleteResponse("执行完成", request.getSessionId()));
    }

    @Override
    public String getType() {
        return "loop";
    }

    private JsonNode call(AiFlowVO flow, String prompt, WorkEventSink sink, String role, ExecuteRequestEntity request, Integer round, Integer pace) {
        try {
            ChatClient client = armory.resolve(flow.getClientId());
            JsonNode node;
            try (McpToolHandle tools = mcpToolService.augmentMcpTool(request.getUserId(), flow.getClientId())) {
                node = parser.parseObject(chatGateway.complete(client, messageBuilder.build(request.getUserId(), request.getSessionId(), flow.getClientId(), prompt, request.getRagTag(), "work"), tools.toolCallbackProvider()));
            }
            emitObject(node, sink, role, request.getSessionId(), round, pace);
            return node;
        } catch (Exception e) {
            ExecuteResponseEntity response = response(role, role + "_exception", e.getMessage() == null ? "执行异常" : e.getMessage(), request.getSessionId(), round, pace);
            sink.message(response);
            return parser.parseObject("{\"" + role + "_exception\":\"" + escape(response.getSectionContent()) + "\"}");
        }
    }

    private void emitObject(JsonNode node, WorkEventSink sink, String role, String sessionId, Integer round, Integer pace) {
        node.fields().forEachRemaining(entry -> sink.message(response(role, entry.getKey(), entry.getValue().asText(), sessionId, round, pace)));
    }

    private ExecuteResponseEntity response(String role, String sectionType, String content, String sessionId, Integer round, Integer pace) {
        return switch (role) {
            case "analyzer" -> ExecuteResponseEntity.createAnalyzerResponse(sectionType, content, round, sessionId);
            case "performer" -> ExecuteResponseEntity.createPerformerResponse(sectionType, content, round, sessionId);
            case "supervisor" -> ExecuteResponseEntity.createSupervisorResponse(sectionType, content, round, sessionId);
            case "summarizer" -> ExecuteResponseEntity.createSummarizerResponse(sectionType, content, round, sessionId);
            default -> ExecuteResponseEntity.createResponse(role, sectionType, content, round, pace, null, sessionId);
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
