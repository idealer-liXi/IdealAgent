package com.idealagent.trigger.controller;

import com.idealagent.domain.ai.model.dto.WorkRequestDTO;
import com.idealagent.domain.ai.service.chat.ChatService;
import com.idealagent.domain.ai.service.rag.RagService;
import com.idealagent.domain.ai.service.work.WorkEventSink;
import com.idealagent.domain.ai.service.work.WorkService;
import com.idealagent.domain.user.model.vo.AuthUserVO;
import com.idealagent.trigger.context.UserContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.concurrent.Executor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.same;
import static org.mockito.Mockito.verify;

class AiControllerWorkExecuteTest {
    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    void executeWorkReturnsEmitterAndDelegatesToWorkService() {
        ChatService chatService = mock(ChatService.class);
        RagService ragService = mock(RagService.class);
        WorkService workService = mock(WorkService.class);
        Executor directExecutor = Runnable::run;
        AiController controller = new AiController(chatService, ragService, workService, directExecutor);
        WorkRequestDTO request = new WorkRequestDTO("agent_default_step", "默认任务执行智能体", "执行任务", null, null, null, null, null);
        UserContext.set(new AuthUserVO(7L, "admin", "admin"));

        SseEmitter emitter = controller.executeWork(request);

        assertThat(emitter).isNotNull();
        verify(workService).execute(eq(7L), same(request), any(WorkEventSink.class));
    }

    @Test
    void workAgentsDelegatesToWorkService() {
        ChatService chatService = mock(ChatService.class);
        RagService ragService = mock(RagService.class);
        WorkService workService = mock(WorkService.class);
        Executor directExecutor = Runnable::run;
        AiController controller = new AiController(chatService, ragService, workService, directExecutor);

        controller.workAgents();

        verify(workService).listAgents();
    }
}
