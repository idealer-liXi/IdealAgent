package com.idealagent.domain.ai.service.work.step;

import com.idealagent.domain.ai.model.entity.ExecuteRequestEntity;
import com.idealagent.domain.ai.model.entity.ExecuteResponseEntity;
import com.idealagent.domain.ai.model.entity.WorkAgent;
import com.idealagent.domain.ai.model.vo.AiFlowVO;
import com.idealagent.domain.ai.repository.IWorkAgentRepository;
import com.idealagent.domain.ai.service.armory.IChatClientArmory;
import com.idealagent.domain.ai.service.work.WorkChatGateway;
import com.idealagent.domain.ai.service.work.WorkEventSink;
import com.idealagent.domain.ai.service.work.WorkJsonParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;

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
    private FakeGateway gateway;
    private RecordingSink sink;
    private ExecuteStepStrategy strategy;

    @BeforeEach
    void setUp() {
        repository = new FakeRepository();
        armory = new FakeArmory();
        gateway = new FakeGateway();
        WorkJsonParser parser = new WorkJsonParser();
        strategy = new ExecuteStepStrategy(
                repository,
                new StepInspectorNode(armory, gateway, parser),
                new StepPlannerNode(armory, gateway, parser),
                new StepRunnerNode(armory, gateway, parser),
                new StepReplierNode(armory, gateway, parser));
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
        request.setAgentId("agent_default_step");
        request.setSessionId("session_work");
        request.setUserMessage("查天气");
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

        @Override
        public String complete(ChatClient client, String prompt) {
            prompts.add(prompt);
            return responses.remove();
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
