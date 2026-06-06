package com.idealagent.domain.ai.service.work.step;

import com.idealagent.domain.ai.model.entity.ExecuteRequestEntity;
import com.idealagent.domain.ai.model.entity.ExecuteResponseEntity;
import com.idealagent.domain.ai.model.entity.WorkAgent;
import com.idealagent.domain.ai.model.vo.AiFlowVO;
import com.idealagent.domain.ai.repository.IWorkAgentRepository;
import com.idealagent.domain.ai.service.armory.IChatClientArmory;
import com.idealagent.domain.ai.service.augment.IMcpToolService;
import com.idealagent.domain.ai.service.augment.McpToolHandle;
import com.idealagent.domain.ai.service.chat.RuntimeMessageBuilder;
import com.idealagent.domain.ai.service.work.WorkChatGateway;
import com.idealagent.domain.ai.service.work.WorkEventSink;
import com.idealagent.domain.ai.service.work.WorkJsonParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.tool.ToolCallbackProvider;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class ExecuteStepStrategyTest {
    private FakeRepository repository;
    private FakeArmory armory;
    private FakeMcpToolService mcpToolService;
    private FakeGateway gateway;
    private FakeMessageBuilder messageBuilder;
    private RecordingSink sink;
    private ExecuteStepStrategy strategy;

    @BeforeEach
    void setUp() {
        repository = new FakeRepository();
        armory = new FakeArmory();
        mcpToolService = new FakeMcpToolService();
        gateway = new FakeGateway();
        messageBuilder = new FakeMessageBuilder();
        WorkJsonParser parser = new WorkJsonParser();
        strategy = new ExecuteStepStrategy(
                repository,
                new StepInspectorNode(armory, mcpToolService, gateway, parser, messageBuilder),
                new StepPlannerNode(armory, mcpToolService, gateway, parser, messageBuilder),
                new StepRunnerNode(armory, mcpToolService, gateway, parser, messageBuilder),
                new StepReplierNode(armory, mcpToolService, gateway, parser, messageBuilder));
        sink = new RecordingSink();
    }

    @Test
    void executesStepNodesAndEmitsComplete() {
        gateway.responses.add("[{\"tool\":\"amap\"}]");
        gateway.responses.add("[{\"step\":\"1\",\"goal\":\"查询天气\"}]");
        gateway.responses.add("{\"runner_result\":\"晴天\",\"runner_status\":\"SUCCESS\"}");
        gateway.responses.add("{\"replier_overview\":\"今天是晴天\"}");

        strategy.execute(request(), sink);

        assertThat(gateway.prompts).hasSize(4);
        assertThat(armory.clientIds).containsExactly("client_inspector", "client_planner", "client_runner", "client_replier");
        assertThat(mcpToolService.clientIds).containsExactly("client_inspector", "client_planner", "client_runner", "client_replier");
        assertThat(mcpToolService.userIds).containsExactly(7L, 7L, 7L, 7L);
        assertThat(mcpToolService.closedCount).isEqualTo(4);
        assertThat(gateway.toolCallbackProviders).hasSize(4);
        assertThat(messageBuilder.ragTags).containsExactly("work-docs", "work-docs", "work-docs", "work-docs");
        assertThat(messageBuilder.messageTypes).containsOnly("work");
        assertThat(sink.messages).extracting(ExecuteResponseEntity::getSectionType)
                .contains("inspector_mcp", "planner_step", "runner_result", "runner_status", "replier_overview");
        assertThat(sink.messages).extracting(ExecuteResponseEntity::getClientType)
                .contains("inspector", "planner", "runner", "replier");
        assertThat(sink.completed).isTrue();
    }

    @Test
    void retriesRunnerFailureThenSucceeds() {
        gateway.responses.add("[]");
        gateway.responses.add("[{\"step\":\"1\"}]");
        gateway.responses.add("{\"runner_result\":\"bad\",\"runner_status\":\"FAIL\"}");
        gateway.responses.add("{\"runner_result\":\"ok\",\"runner_status\":\"SUCCESS\"}");
        gateway.responses.add("{\"replier_overview\":\"完成\"}");

        strategy.execute(request(), sink);

        assertThat(gateway.prompts).hasSize(5);
        assertThat(sink.messages).extracting(ExecuteResponseEntity::getSectionType).contains("runner_result", "replier_overview");
    }

    private ExecuteRequestEntity request() {
        ExecuteRequestEntity request = new ExecuteRequestEntity();
        request.setUserId(7L);
        request.setAgentId("agent_default_step");
        request.setSessionId("session_work");
        request.setUserMessage("查天气");
        request.setRagTag("work-docs");
        request.setMaxRetry(2);
        return request;
    }

    private static class FakeRepository implements IWorkAgentRepository {
        @Override
        public WorkAgent findAgent(String agentId) {
            return null;
        }

        @Override
        public List<WorkAgent> listEnabledAgents() {
            return List.of();
        }

        @Override
        public String findExecuteType(String agentId) {
            return "step";
        }

        @Override
        public Map<String, AiFlowVO> listFlowMap(String agentId) {
            Map<String, AiFlowVO> flows = new LinkedHashMap<>();
            flows.put("inspector", flow("client_inspector", "inspect %s"));
            flows.put("planner", flow("client_planner", "plan %s %s"));
            flows.put("runner", flow("client_runner", "run %s %s %s"));
            flows.put("replier", flow("client_replier", "reply %s %s"));
            return flows;
        }

        private AiFlowVO flow(String clientId, String prompt) {
            AiFlowVO vo = new AiFlowVO();
            vo.setAgentId("agent_default_step");
            vo.setClientId(clientId);
            vo.setUserPrompt(prompt);
            return vo;
        }
    }

    private static class FakeArmory implements IChatClientArmory {
        private final List<String> clientIds = new ArrayList<>();
        private final ChatClient client = mock(ChatClient.class);

        @Override
        public ChatClient resolve(String clientId) {
            clientIds.add(clientId);
            return client;
        }
    }

    private static class FakeGateway extends WorkChatGateway {
        private final Queue<String> responses = new ArrayDeque<>();
        private final List<String> prompts = new ArrayList<>();
        private final List<ToolCallbackProvider> toolCallbackProviders = new ArrayList<>();

        @Override
        public String complete(ChatClient client, String prompt, ToolCallbackProvider toolCallbackProvider) {
            prompts.add(prompt);
            toolCallbackProviders.add(toolCallbackProvider);
            return responses.remove();
        }

        @Override
        public String complete(ChatClient client, List<Message> messages, ToolCallbackProvider toolCallbackProvider) {
            prompts.add(messages.get(messages.size() - 1).getText());
            toolCallbackProviders.add(toolCallbackProvider);
            return responses.remove();
        }
    }

    private static class FakeMessageBuilder extends RuntimeMessageBuilder {
        private final List<String> ragTags = new ArrayList<>();
        private final List<String> messageTypes = new ArrayList<>();

        FakeMessageBuilder() {
            super(null, null, null);
        }

        @Override
        public List<Message> build(Long userId, String sessionId, String clientId, String content, String ragTag, String messageType) {
            ragTags.add(ragTag);
            messageTypes.add(messageType);
            return List.of(new UserMessage(content));
        }
    }

    private static class FakeMcpToolService implements IMcpToolService {
        private final List<Long> userIds = new ArrayList<>();
        private final List<String> clientIds = new ArrayList<>();
        private int closedCount;

        @Override
        public McpToolHandle augmentMcpTool(Long userId, String clientId) {
            userIds.add(userId);
            clientIds.add(clientId);
            return new McpToolHandle(new SyncMcpToolCallbackProvider(), List.of()) {
                @Override
                public void close() {
                    closedCount++;
                }
            };
        }
    }

    private static class RecordingSink implements WorkEventSink {
        private final List<ExecuteResponseEntity> messages = new ArrayList<>();
        private boolean completed;

        @Override
        public void message(ExecuteResponseEntity response) {
            messages.add(response);
        }

        @Override
        public void complete(ExecuteResponseEntity response) {
            completed = true;
        }

        @Override
        public void error(String message) {
            throw new AssertionError(message);
        }
    }
}
