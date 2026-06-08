package com.idealagent.domain.ai.service.work;

import com.idealagent.domain.ai.model.dto.WorkRequestDTO;
import com.idealagent.domain.ai.model.entity.ExecuteRequestEntity;
import com.idealagent.domain.ai.model.entity.ExecuteResponseEntity;
import com.idealagent.domain.ai.model.entity.WorkAgent;
import com.idealagent.domain.ai.model.vo.AiFlowVO;
import com.idealagent.domain.ai.repository.IWorkAgentRepository;
import com.idealagent.domain.session.model.entity.ChatMessage;
import com.idealagent.domain.session.model.entity.ChatSession;
import com.idealagent.domain.session.model.vo.ChatMessageVO;
import com.idealagent.domain.session.model.vo.ChatSessionVO;
import com.idealagent.domain.session.repository.ISessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WorkServiceTest {
    private FakeSessionRepository sessionRepository;
    private FakeWorkAgentRepository agentRepository;
    private RecordingDispatchService dispatchService;
    private FakeMatchChecker matchChecker;
    private WorkService workService;

    @BeforeEach
    void setUp() {
        sessionRepository = new FakeSessionRepository();
        agentRepository = new FakeWorkAgentRepository();
        dispatchService = new RecordingDispatchService();
        matchChecker = new FakeMatchChecker();
        workService = new WorkService(sessionRepository, agentRepository, dispatchService, matchChecker);
    }

    @Test
    void createsWorkSessionPersistsUserMessageAndDispatches() {
        agentRepository.completeStepFlow();

        workService.execute(7L, request(null, "执行任务"), new RecordingSink());

        assertThat(sessionRepository.sessions).hasSize(1);
        assertThat(sessionRepository.sessions.get(0).getType()).isEqualTo("work");
        assertThat(sessionRepository.messages).extracting(ChatMessage::getRole).containsExactly("user");
        assertThat(dispatchService.lastRequest.getUserId()).isEqualTo(7L);
        assertThat(dispatchService.lastRequest.getRagTag()).isNull();
        assertThat(dispatchService.lastRequest.getMaxRetry()).isEqualTo(2);
    }

    @Test
    void persistsWorkEventAndAssistantOverviewMessages() {
        dispatchService.overviewSection = "summarizer_overview";
        agentRepository.completeStepFlow();

        workService.execute(7L, request(null, "执行任务"), new RecordingSink());

        assertThat(sessionRepository.messages).extracting(ChatMessage::getRole).containsExactly("user", "event", "assistant");
        assertThat(sessionRepository.messages.get(1).getContent()).contains("summarizer_overview");
        assertThat(sessionRepository.messages.get(2).getContent()).isEqualTo("执行完成");
    }

    @Test
    void persistsWorkCompleteEventMessages() {
        dispatchService.complete = true;
        agentRepository.completeStepFlow();

        workService.execute(7L, request(null, "执行任务"), new RecordingSink());

        assertThat(sessionRepository.messages).extracting(ChatMessage::getRole).containsExactly("user", "event");
        assertThat(sessionRepository.messages.get(1).getContent()).contains("执行完成");
    }

    @Test
    void listsPersistedWorkSessionsAndMessages() {
        dispatchService.overviewSection = "replier_overview";
        agentRepository.completeStepFlow();
        workService.execute(7L, request(null, "发送新闻邮件"), new RecordingSink());
        String sessionId = sessionRepository.sessions.get(0).getSessionId();

        List<ChatSessionVO> sessions = workService.listSessions(7L);
        List<ChatMessageVO> messages = workService.listMessages(7L, sessionId);

        assertThat(sessions).extracting(ChatSessionVO::sessionId).containsExactly(sessionId);
        assertThat(messages).extracting(ChatMessageVO::role).containsExactly("user", "event", "assistant");
        assertThat(messages).extracting(ChatMessageVO::content).anySatisfy(content -> assertThat(content).contains("replier_overview"));
    }

    @Test
    void rejectsBlankMessage() {
        assertThatThrownBy(() -> workService.execute(7L, request(null, " "), new RecordingSink()))
                .isInstanceOf(WorkException.class)
                .hasMessage("任务内容不能为空");
    }

    @Test
    void rejectsDisabledAgent() {
        agentRepository.agent.setStatus(0);

        assertThatThrownBy(() -> workService.execute(7L, request(null, "执行任务"), new RecordingSink()))
                .isInstanceOf(WorkException.class)
                .hasMessage("Work Agent 不可用");
    }

    @Test
    void rejectsUnmatchedTask() {
        matchChecker.matched = false;
        agentRepository.completeStepFlow();

        assertThatThrownBy(() -> workService.execute(7L, request(null, "执行任务"), new RecordingSink()))
                .isInstanceOf(WorkException.class)
                .hasMessage("当前任务需求与智能体定位不匹配，请更换智能体或调整需求");
    }

    @Test
    void rejectsExistingNonWorkSession() {
        agentRepository.completeStepFlow();
        ChatSession chat = new ChatSession();
        chat.setSessionId("session_existing");
        chat.setUserId(7L);
        chat.setType("chat");
        sessionRepository.sessions.add(chat);

        assertThatThrownBy(() -> workService.execute(7L, request("session_existing", "执行任务"), new RecordingSink()))
                .isInstanceOf(WorkException.class)
                .hasMessage("会话类型不匹配");
    }

    @Test
    void rejectsAgentWithIncompleteStrategyFlow() {
        agentRepository.flow("inspector", 1);

        assertThatThrownBy(() -> workService.execute(7L, request(null, "执行任务"), new RecordingSink()))
                .isInstanceOf(WorkException.class)
                .hasMessage("Agent Flow 未完整配置，缺少角色：planner, runner, replier");
    }

    @Test
    void listsOnlyAgentsWithCompleteStrategyFlow() {
        WorkAgent incomplete = new WorkAgent();
        incomplete.setAgentId("agent_incomplete_step");
        incomplete.setAgentName("Incomplete");
        incomplete.setAgentDesc("未完整配置");
        incomplete.setAgentType("step");
        incomplete.setStatus(1);
        agentRepository.agents.add(incomplete);
        agentRepository.completeStepFlow();

        assertThat(workService.listAgents()).extracting("agentId").containsExactly("agent_default_step");
    }

    private WorkRequestDTO request(String sessionId, String message) {
        return new WorkRequestDTO("agent_default_step", "默认任务执行智能体", message, "work-docs", sessionId, null, null, null);
    }

    private static class FakeSessionRepository implements ISessionRepository {
        private final List<ChatSession> sessions = new ArrayList<>();
        private final List<ChatMessage> messages = new ArrayList<>();

        @Override
        public Optional<ChatSession> findSession(String sessionId, Long userId) {
            return sessions.stream().filter(s -> s.getSessionId().equals(sessionId) && s.getUserId().equals(userId)).findFirst();
        }

        @Override
        public Optional<ChatSession> findSession(String sessionId, Long userId, String type) {
            return findSession(sessionId, userId).filter(s -> type.equals(s.getType()));
        }

        @Override
        public ChatSession saveSession(ChatSession session) {
            sessions.add(session);
            return session;
        }

        @Override
        public List<ChatSession> listSessions(Long userId) {
            return sessions;
        }

        @Override
        public List<ChatSession> listSessions(Long userId, String type) {
            return sessions.stream().filter(s -> type.equals(s.getType())).toList();
        }

        @Override
        public ChatMessage saveMessage(ChatMessage message) {
            messages.add(message);
            return message;
        }

        @Override
        public List<ChatMessage> listMessages(String sessionId, Long userId) {
            return messages;
        }
    }

    private static class FakeWorkAgentRepository implements IWorkAgentRepository {
        private final WorkAgent agent = new WorkAgent();
        private final List<WorkAgent> agents = new ArrayList<>();
        private final Map<String, Map<String, AiFlowVO>> flows = new java.util.HashMap<>();

        FakeWorkAgentRepository() {
            agent.setAgentId("agent_default_step");
            agent.setAgentName("Default Step");
            agent.setAgentDesc("默认任务执行智能体");
            agent.setAgentType("step");
            agent.setStatus(1);
            agents.add(agent);
        }

        void completeStepFlow() {
            flow("inspector", 1);
            flow("planner", 2);
            flow("runner", 3);
            flow("replier", 4);
        }

        void flow(String role, int seq) {
            AiFlowVO flow = new AiFlowVO();
            flow.setAgentId("agent_default_step");
            flow.setClientId("client_" + role);
            flow.setClientRole(role);
            flow.setFlowSeq(seq);
            flow.setUserPrompt(role + " prompt");
            flows.computeIfAbsent("agent_default_step", ignored -> new java.util.HashMap<>()).put(role, flow);
        }

        @Override
        public WorkAgent findAgent(String agentId) {
            return agent;
        }

        @Override
        public List<WorkAgent> listEnabledAgents() {
            return agents;
        }

        @Override
        public String findExecuteType(String agentId) {
            return "step";
        }

        @Override
        public Map<String, AiFlowVO> listFlowMap(String agentId) {
            return flows.getOrDefault(agentId, Map.of());
        }
    }

    private static class RecordingDispatchService extends WorkDispatchService {
        private ExecuteRequestEntity lastRequest;
        private String overviewSection;
        private boolean complete;

        RecordingDispatchService() {
            super(null, List.of());
        }

        @Override
        public void dispatch(ExecuteRequestEntity request, WorkEventSink sink) {
            lastRequest = request;
            if (overviewSection != null) {
                sink.message(ExecuteResponseEntity.section(overviewSection, "执行完成", null, request.getSessionId()));
            }
            if (complete) {
                sink.complete(ExecuteResponseEntity.complete(request.getSessionId()));
            }
        }
    }

    private static class FakeMatchChecker extends MatchChecker {
        private boolean matched = true;

        FakeMatchChecker() {
            super(null, null, null);
        }

        @Override
        public boolean isTaskMatched(String agentId, String agentDesc, String userMessage) {
            return matched;
        }
    }

    private static class RecordingSink implements WorkEventSink {
        @Override
        public void message(ExecuteResponseEntity response) {
        }

        @Override
        public void complete(ExecuteResponseEntity response) {
        }

        @Override
        public void error(String message) {
        }
    }
}
