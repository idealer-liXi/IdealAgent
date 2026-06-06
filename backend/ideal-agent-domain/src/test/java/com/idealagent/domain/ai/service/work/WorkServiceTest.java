package com.idealagent.domain.ai.service.work;

import com.idealagent.domain.ai.model.dto.WorkRequestDTO;
import com.idealagent.domain.ai.model.entity.ExecuteRequestEntity;
import com.idealagent.domain.ai.model.entity.ExecuteResponseEntity;
import com.idealagent.domain.ai.model.entity.WorkAgent;
import com.idealagent.domain.ai.model.vo.AiFlowVO;
import com.idealagent.domain.ai.repository.IWorkAgentRepository;
import com.idealagent.domain.session.model.entity.ChatMessage;
import com.idealagent.domain.session.model.entity.ChatSession;
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
        workService.execute(7L, request(null, "执行任务"), new RecordingSink());

        assertThat(sessionRepository.sessions).hasSize(1);
        assertThat(sessionRepository.sessions.get(0).getType()).isEqualTo("work");
        assertThat(sessionRepository.messages).extracting(ChatMessage::getRole).containsExactly("user");
        assertThat(dispatchService.lastRequest.getUserId()).isEqualTo(7L);
        assertThat(dispatchService.lastRequest.getRagTag()).isEqualTo("work-docs");
        assertThat(dispatchService.lastRequest.getMaxRetry()).isEqualTo(2);
    }

    @Test
    void persistsWorkEventAndAssistantOverviewMessages() {
        dispatchService.overviewSection = "summarizer_overview";

        workService.execute(7L, request(null, "执行任务"), new RecordingSink());

        assertThat(sessionRepository.messages).extracting(ChatMessage::getRole).containsExactly("user", "event", "assistant");
        assertThat(sessionRepository.messages.get(1).getContent()).contains("summarizer_overview");
        assertThat(sessionRepository.messages.get(2).getContent()).isEqualTo("执行完成");
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

        assertThatThrownBy(() -> workService.execute(7L, request(null, "执行任务"), new RecordingSink()))
                .isInstanceOf(WorkException.class)
                .hasMessage("当前任务需求与智能体定位不匹配，请更换智能体或调整需求");
    }

    @Test
    void rejectsExistingNonWorkSession() {
        ChatSession chat = new ChatSession();
        chat.setSessionId("session_existing");
        chat.setUserId(7L);
        chat.setType("chat");
        sessionRepository.sessions.add(chat);

        assertThatThrownBy(() -> workService.execute(7L, request("session_existing", "执行任务"), new RecordingSink()))
                .isInstanceOf(WorkException.class)
                .hasMessage("会话类型不匹配");
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

        FakeWorkAgentRepository() {
            agent.setAgentId("agent_default_step");
            agent.setAgentDesc("默认任务执行智能体");
            agent.setAgentType("step");
            agent.setStatus(1);
        }

        @Override
        public WorkAgent findAgent(String agentId) {
            return agent;
        }

        @Override
        public List<WorkAgent> listEnabledAgents() {
            return List.of(agent);
        }

        @Override
        public String findExecuteType(String agentId) {
            return "step";
        }

        @Override
        public Map<String, AiFlowVO> listFlowMap(String agentId) {
            return Map.of();
        }
    }

    private static class RecordingDispatchService extends WorkDispatchService {
        private ExecuteRequestEntity lastRequest;
        private String overviewSection;

        RecordingDispatchService() {
            super(null, List.of());
        }

        @Override
        public void dispatch(ExecuteRequestEntity request, WorkEventSink sink) {
            lastRequest = request;
            if (overviewSection != null) {
                sink.message(ExecuteResponseEntity.section(overviewSection, "执行完成", null, request.getSessionId()));
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
