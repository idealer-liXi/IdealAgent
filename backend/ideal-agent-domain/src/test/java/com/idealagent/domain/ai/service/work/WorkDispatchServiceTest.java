package com.idealagent.domain.ai.service.work;

import com.idealagent.domain.ai.model.entity.ExecuteRequestEntity;
import com.idealagent.domain.ai.repository.IWorkAgentRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WorkDispatchServiceTest {
    private final IWorkAgentRepository repository = mock(IWorkAgentRepository.class);
    private final RecordingStrategy stepStrategy = new RecordingStrategy("step");
    private final RecordingStrategy loopStrategy = new RecordingStrategy("loop");
    private final RecordingStrategy reactStrategy = new RecordingStrategy("react");
    private final WorkDispatchService dispatchService = new WorkDispatchService(repository, List.of(stepStrategy, loopStrategy, reactStrategy));

    @Test
    void dispatchesStepStrategyByAgentType() {
        ExecuteRequestEntity request = new ExecuteRequestEntity();
        request.setAgentId("agent_default_step");
        when(repository.findExecuteType("agent_default_step")).thenReturn("step");

        dispatchService.dispatch(request, new RecordingSink());

        assertThat(stepStrategy.called).isTrue();
    }

    @Test
    void dispatchesLoopAndReactStrategiesByAgentType() {
        ExecuteRequestEntity loopRequest = new ExecuteRequestEntity();
        loopRequest.setAgentId("agent_loop");
        ExecuteRequestEntity reactRequest = new ExecuteRequestEntity();
        reactRequest.setAgentId("agent_react");
        when(repository.findExecuteType("agent_loop")).thenReturn("loop");
        when(repository.findExecuteType("agent_react")).thenReturn("react");

        dispatchService.dispatch(loopRequest, new RecordingSink());
        dispatchService.dispatch(reactRequest, new RecordingSink());

        assertThat(loopStrategy.called).isTrue();
        assertThat(reactStrategy.called).isTrue();
    }

    @Test
    void rejectsUnsupportedStrategy() {
        ExecuteRequestEntity request = new ExecuteRequestEntity();
        request.setAgentId("agent_unknown");
        when(repository.findExecuteType("agent_unknown")).thenReturn("unknown");

        assertThatThrownBy(() -> dispatchService.dispatch(request, new RecordingSink()))
                .isInstanceOf(WorkException.class)
                .hasMessage("Work 执行策略不支持");
    }

    private static class RecordingStrategy implements IExecuteStrategy {
        private final String type;
        private boolean called;

        RecordingStrategy(String type) {
            this.type = type;
        }

        @Override
        public void execute(ExecuteRequestEntity request, WorkEventSink sink) {
            called = true;
        }

        @Override
        public String getType() {
            return type;
        }
    }

    private static class RecordingSink implements WorkEventSink {
        @Override
        public void message(com.idealagent.domain.ai.model.entity.ExecuteResponseEntity response) {
        }

        @Override
        public void complete(com.idealagent.domain.ai.model.entity.ExecuteResponseEntity response) {
        }

        @Override
        public void error(String message) {
        }
    }
}
