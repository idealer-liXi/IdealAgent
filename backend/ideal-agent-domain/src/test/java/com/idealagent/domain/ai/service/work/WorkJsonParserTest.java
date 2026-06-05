package com.idealagent.domain.ai.service.work;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WorkJsonParserTest {
    private final WorkJsonParser parser = new WorkJsonParser();

    @Test
    void extractsArrayAfterThinkAndFence() {
        JsonNode node = parser.parseArray("<think>ignore</think>```json\n[{\"tool\":\"amap\"}]\n```");

        assertThat(node.isArray()).isTrue();
        assertThat(node.get(0).get("tool").asText()).isEqualTo("amap");
    }

    @Test
    void extractsObjectFromMixedText() {
        JsonNode node = parser.parseObject("prefix {\"runner_result\":\"ok\",\"runner_status\":\"SUCCESS\"} suffix");

        assertThat(node.get("runner_result").asText()).isEqualTo("ok");
        assertThat(node.get("runner_status").asText()).isEqualTo("SUCCESS");
    }

    @Test
    void rejectsMissingJsonArray() {
        assertThatThrownBy(() -> parser.parseArray("no json here"))
                .isInstanceOf(WorkException.class)
                .hasMessage("JSON 数组解析失败");
    }
}
