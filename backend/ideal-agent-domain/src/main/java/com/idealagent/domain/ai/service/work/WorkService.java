package com.idealagent.domain.ai.service.work;

import com.idealagent.domain.ai.model.dto.WorkRequestDTO;
import com.idealagent.domain.ai.model.entity.ExecuteRequestEntity;
import com.idealagent.domain.ai.model.entity.ExecuteResponseEntity;
import com.idealagent.domain.ai.model.entity.WorkAgent;
import com.idealagent.domain.ai.model.vo.WorkAgentOptionVO;
import com.idealagent.domain.ai.repository.IWorkAgentRepository;
import com.idealagent.domain.session.model.entity.ChatMessage;
import com.idealagent.domain.session.model.entity.ChatSession;
import com.idealagent.domain.session.repository.ISessionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class WorkService {
    private static final String WORK_TYPE = "work";
    private static final String USER_ROLE = "user";
    private static final String EVENT_ROLE = "event";
    private static final String ASSISTANT_ROLE = "assistant";
    private static final String SUMMARIZER_OVERVIEW = "summarizer_overview";
    private static final String REPLIER_OVERVIEW = "replier_overview";
    private static final String EVALUATOR_OVERVIEW = "evaluator_overview";
    private static final int ENABLED = 1;

    private final ISessionRepository sessionRepository;
    private final IWorkAgentRepository workAgentRepository;
    private final WorkDispatchService dispatchService;
    private final MatchChecker matchChecker;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public WorkService(ISessionRepository sessionRepository, IWorkAgentRepository workAgentRepository, WorkDispatchService dispatchService, MatchChecker matchChecker) {
        this.sessionRepository = sessionRepository;
        this.workAgentRepository = workAgentRepository;
        this.dispatchService = dispatchService;
        this.matchChecker = matchChecker;
    }

    public List<WorkAgentOptionVO> listAgents() {
        return workAgentRepository.listEnabledAgents().stream()
                .map(agent -> new WorkAgentOptionVO(
                        agent.getAgentId(),
                        StringUtils.hasText(agent.getAgentName()) ? agent.getAgentName() : agent.getAgentId(),
                        agent.getAgentType(),
                        agent.getAgentDesc()))
                .toList();
    }

    public void execute(Long userId, WorkRequestDTO request, WorkEventSink sink) {
        validate(userId, request);
        WorkAgent agent = requireAgent(request.agentId());
        validateAgentDesc(request.agentDesc(), agent.getAgentDesc());
        if (!matchChecker.isTaskMatched(agent.getAgentId(), agent.getAgentDesc(), request.userMessage())) {
            throw new WorkException("当前任务需求与智能体定位不匹配，请更换智能体或调整需求");
        }
        String sessionId = ensureSession(userId, request, agent);
        sessionRepository.saveMessage(message(sessionId, USER_ROLE, request.userMessage()));

        ExecuteRequestEntity executeRequest = new ExecuteRequestEntity();
        executeRequest.setUserId(userId);
        executeRequest.setAgentId(agent.getAgentId());
        executeRequest.setUserMessage(request.userMessage());
        executeRequest.setRagTag(request.ragTag());
        executeRequest.setSessionId(sessionId);
        executeRequest.setMaxRound(request.maxRound());
        executeRequest.setMaxRetry(safeRetry(request.maxRetry()));
        executeRequest.setMaxPace(request.maxPace());
        dispatchService.dispatch(executeRequest, persistingSink(sessionId, sink));
    }

    private WorkEventSink persistingSink(String sessionId, WorkEventSink delegate) {
        return new WorkEventSink() {
            @Override
            public void message(ExecuteResponseEntity response) {
                sessionRepository.saveMessage(WorkService.this.message(sessionId, EVENT_ROLE, toJson(response)));
                if (isAssistantOverview(response.getSectionType())) {
                    sessionRepository.saveMessage(WorkService.this.message(sessionId, ASSISTANT_ROLE, response.getSectionContent()));
                }
                delegate.message(response);
            }

            @Override
            public void complete(ExecuteResponseEntity response) {
                delegate.complete(response);
            }

            @Override
            public void error(String message) {
                delegate.error(message);
            }
        };
    }

    private boolean isAssistantOverview(String sectionType) {
        return SUMMARIZER_OVERVIEW.equals(sectionType)
                || REPLIER_OVERVIEW.equals(sectionType)
                || EVALUATOR_OVERVIEW.equals(sectionType);
    }

    private String toJson(ExecuteResponseEntity response) {
        try {
            return objectMapper.writeValueAsString(response);
        } catch (Exception e) {
            throw new WorkException("Work 消息序列化失败", e);
        }
    }

    private void validate(Long userId, WorkRequestDTO request) {
        if (userId == null) {
            throw new WorkException("用户未登录");
        }
        if (request == null || !StringUtils.hasText(request.userMessage())) {
            throw new WorkException("任务内容不能为空");
        }
        if (!StringUtils.hasText(request.agentId())) {
            throw new WorkException("Work Agent 不能为空");
        }
    }

    private WorkAgent requireAgent(String agentId) {
        WorkAgent agent = workAgentRepository.findAgent(agentId);
        if (agent == null || agent.getStatus() == null || agent.getStatus() != ENABLED) {
            throw new WorkException("Work Agent 不可用");
        }
        return agent;
    }

    private void validateAgentDesc(String requestDesc, String storedDesc) {
        if (StringUtils.hasText(requestDesc) && StringUtils.hasText(storedDesc) && !requestDesc.equals(storedDesc)) {
            throw new WorkException("Work Agent 描述不匹配");
        }
    }

    private String ensureSession(Long userId, WorkRequestDTO request, WorkAgent agent) {
        String sessionId = StringUtils.hasText(request.sessionId()) ? request.sessionId() : id("session_");
        if (sessionRepository.findSession(sessionId, userId).isPresent()) {
            if (sessionRepository.findSession(sessionId, userId, WORK_TYPE).isEmpty()) {
                throw new WorkException("会话类型不匹配");
            }
            return sessionId;
        }
        ChatSession session = new ChatSession();
        session.setSessionId(sessionId);
        session.setType(WORK_TYPE);
        session.setTitle(titleOf(request.userMessage()));
        session.setUserId(userId);
        session.setTargetId(agent.getAgentId());
        sessionRepository.saveSession(session);
        return sessionId;
    }

    private ChatMessage message(String sessionId, String role, String content) {
        ChatMessage message = new ChatMessage();
        message.setMessageId(id("msg_"));
        message.setSessionId(sessionId);
        message.setType(WORK_TYPE);
        message.setRole(role);
        message.setContent(content);
        message.setCreateTime(LocalDateTime.now());
        return message;
    }

    private int safeRetry(Integer value) {
        if (value == null) {
            return 2;
        }
        return Math.max(1, Math.min(5, value));
    }

    private String titleOf(String content) {
        String trimmed = content.trim();
        return trimmed.length() > 24 ? trimmed.substring(0, 24) : trimmed;
    }

    private String id(String prefix) {
        return prefix + UUID.randomUUID().toString().replace("-", "");
    }
}
