package com.idealagent.domain.ai.service.work;

import com.idealagent.domain.ai.model.entity.ExecuteRequestEntity;
import com.idealagent.domain.ai.model.entity.ExecuteResponseEntity;
import com.idealagent.domain.ai.model.vo.AiFlowVO;
import com.idealagent.domain.ai.repository.IWorkAgentRepository;
import com.idealagent.domain.ai.service.armory.IChatClientArmory;
import com.idealagent.domain.ai.service.work.loop.ExecuteLoopStrategy;
import com.idealagent.domain.ai.service.work.react.ExecuteReactStrategy;
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

class MiniAgentParityStrategyTest {
    @Test
    void loopStrategyRunsAnalyzerPerformerSupervisorSummarizer() {
        FakeGateway gateway = new FakeGateway();
        gateway.responses.add("{\"analyzer_status\":\"RUNNING\",\"analyzer_progress\":\"50\"}");
        gateway.responses.add("{\"performer_result\":\"done\"}");
        gateway.responses.add("{\"supervisor_status\":\"PASS\"}");
        gateway.responses.add("{\"summarizer_overview\":\"summary\"}");
        RecordingSink sink = new RecordingSink();

        new ExecuteLoopStrategy(new FakeRepository(), new FakeArmory(), gateway, new WorkJsonParser()).execute(request("loop"), sink);

        assertThat(gateway.prompts).hasSize(4);
        assertThat(sink.messages).extracting(ExecuteResponseEntity::getClientType)
                .contains("analyzer", "performer", "supervisor", "summarizer");
        assertThat(sink.completed).isTrue();
    }

    @Test
    void reactStrategyRunsObserverReasonerActorEvaluator() {
        FakeGateway gateway = new FakeGateway();
        gateway.responses.add("{\"observer_status\":\"RUNNING\"}");
        gateway.responses.add("{\"reasoner_action\":\"next\"}");
        gateway.responses.add("{\"actor_result\":\"done\"}");
        gateway.responses.add("{\"observer_status\":\"COMPLETED\"}");
        gateway.responses.add("{\"evaluator_overview\":\"ok\"}");
        RecordingSink sink = new RecordingSink();

        new ExecuteReactStrategy(new FakeRepository(), new FakeArmory(), gateway, new WorkJsonParser()).execute(request("react"), sink);

        assertThat(gateway.prompts).hasSize(5);
        assertThat(sink.messages).extracting(ExecuteResponseEntity::getClientType)
                .contains("observer", "reasoner", "actor", "evaluator");
        assertThat(sink.completed).isTrue();
    }

    private ExecuteRequestEntity request(String type) {
        ExecuteRequestEntity request = new ExecuteRequestEntity();
        request.setAgentId("agent_" + type);
        request.setSessionId("session_work");
        request.setUserMessage("执行任务");
        request.setMaxRound(2);
        request.setMaxPace(1);
        request.setMaxRetry(2);
        return request;
    }

    private static class FakeRepository implements IWorkAgentRepository {
        @Override public com.idealagent.domain.ai.model.entity.WorkAgent findAgent(String agentId) { return null; }
        @Override public List<com.idealagent.domain.ai.model.entity.WorkAgent> listEnabledAgents() { return List.of(); }
        @Override public String findExecuteType(String agentId) { return agentId.replace("agent_", ""); }
        @Override public Map<String, AiFlowVO> listFlowMap(String agentId) {
            Map<String, AiFlowVO> flows = new LinkedHashMap<>();
            for (String role : List.of("analyzer", "performer", "supervisor", "summarizer", "observer", "reasoner", "actor", "evaluator")) {
                AiFlowVO flow = new AiFlowVO();
                flow.setAgentId(agentId);
                flow.setClientId("client_" + role);
                flow.setClientRole(role);
                flow.setUserPrompt(role + " %s %s %s %s %s");
                flows.put(role, flow);
            }
            return flows;
        }
    }

    private static class FakeArmory implements IChatClientArmory {
        private final ChatClient client = mock(ChatClient.class);
        @Override public ChatClient resolve(String clientId) { return client; }
    }

    private static class FakeGateway extends WorkChatGateway {
        private final Queue<String> responses = new ArrayDeque<>();
        private final List<String> prompts = new ArrayList<>();
        @Override public String complete(ChatClient client, String prompt) {
            prompts.add(prompt);
            return responses.remove();
        }
    }

    private static class RecordingSink implements WorkEventSink {
        private final List<ExecuteResponseEntity> messages = new ArrayList<>();
        private boolean completed;
        @Override public void message(ExecuteResponseEntity response) { messages.add(response); }
        @Override public void complete(ExecuteResponseEntity response) { completed = true; }
        @Override public void error(String message) { throw new AssertionError(message); }
    }
}
