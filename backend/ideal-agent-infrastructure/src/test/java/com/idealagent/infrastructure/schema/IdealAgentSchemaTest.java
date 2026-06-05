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
}
