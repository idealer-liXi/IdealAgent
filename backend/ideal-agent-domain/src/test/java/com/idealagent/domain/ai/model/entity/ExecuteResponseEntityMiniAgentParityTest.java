package com.idealagent.domain.ai.model.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ExecuteResponseEntityMiniAgentParityTest {
    @Test
    void createsMiniAgentStyleLoopAndReactResponses() {
        ExecuteResponseEntity analyzer = ExecuteResponseEntity.createAnalyzerResponse("analyzer_status", "COMPLETED", 2, "session_1");
        ExecuteResponseEntity actor = ExecuteResponseEntity.createActorResponse("actor_result", "done", 3, "session_1");

        assertThat(analyzer.getClientType()).isEqualTo("analyzer");
        assertThat(analyzer.getRound()).isEqualTo(2);
        assertThat(analyzer.getPace()).isNull();
        assertThat(actor.getClientType()).isEqualTo("actor");
        assertThat(actor.getPace()).isEqualTo(3);
        assertThat(actor.getRound()).isNull();
    }

    @Test
    void createsMiniAgentStyleCompleteResponse() {
        ExecuteResponseEntity complete = ExecuteResponseEntity.createCompleteResponse("执行完成", "session_1");

        assertThat(complete.getClientType()).isEqualTo("complete");
        assertThat(complete.getSectionType()).isNull();
        assertThat(complete.getSectionContent()).isEqualTo("执行完成");
    }
}
