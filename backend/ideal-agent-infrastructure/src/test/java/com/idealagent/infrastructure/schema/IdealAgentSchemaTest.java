package com.idealagent.infrastructure.schema;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class IdealAgentSchemaTest {

    @Test
    void mcpSecretUsesTextColumnForLongCookieSecrets() throws Exception {
        String schema = Files.readString(Path.of("..", "docs", "mysql", "ideal_agent_schema.sql"));

        assertThat(schema).contains("mcp_secret TEXT DEFAULT NULL");
    }

    @Test
    void aiFlowUsesMiniAgentColumns() throws Exception {
        String schema = Files.readString(Path.of("..", "docs", "mysql", "ideal_agent_schema.sql"));

        assertThat(schema).contains("client_role VARCHAR(64) NOT NULL");
        assertThat(schema).contains("user_prompt TEXT NOT NULL");
        assertThat(schema).contains("flow_seq INT NOT NULL DEFAULT 1");
        assertThat(schema).contains("UNIQUE KEY uk_flow_agent_client (agent_id, client_id)");
        assertThat(schema).doesNotContain("flow_id VARCHAR(64) NOT NULL");
        assertThat(schema).doesNotContain("role_type VARCHAR(64) NOT NULL");
        assertThat(schema).doesNotContain("sort_order INT NOT NULL");
    }
}
