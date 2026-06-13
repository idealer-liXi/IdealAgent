package com.idealagent;

import com.idealagent.config.WebMvcConfig;
import com.idealagent.domain.ai.model.enumeration.ConfigKind;
import com.idealagent.domain.ai.model.vo.AiConfigRecordVO;
import com.idealagent.domain.ai.service.config.AiConfigService;
import com.idealagent.domain.user.model.vo.AuthUserVO;
import com.idealagent.domain.user.service.auth.ITokenParser;
import com.idealagent.trigger.controller.AiDisplayController;
import com.idealagent.trigger.interceptor.AuthInterceptor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AiDisplayController.class)
@Import({WebMvcConfig.class, AuthInterceptor.class})
class AiDisplayControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AiConfigService aiConfigService;

    @MockitoBean
    private ITokenParser tokenParser;

    @Test
    void publicMcpListReturnsEnabledMcpWithoutSecrets() throws Exception {
        when(tokenParser.parseToken("token-1")).thenReturn(new AuthUserVO(7L, "alice", "user"));
        when(aiConfigService.list(ConfigKind.MCP)).thenReturn(List.of(
                new AiConfigRecordVO("mcp_mail", "Mail", "sse", "{\"baseUri\":\"http://localhost:9004\"}", "secret", null, 1, 0L, null, null, null, null),
                new AiConfigRecordVO("mcp_disabled", "Disabled", "sse", "{}", "secret", null, 0, 0L, null, null, null, null)));

        mockMvc.perform(get("/ai/mcps").header("Authorization", "Bearer token-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].configId").value("mcp_mail"))
                .andExpect(jsonPath("$.data[0].name").value("Mail"))
                .andExpect(jsonPath("$.data[0].secret").doesNotExist())
                .andExpect(jsonPath("$.data.length()").value(1));
    }

    @Test
    void publicModelListReturnsEnabledModelsWithoutSecrets() throws Exception {
        when(tokenParser.parseToken("token-1")).thenReturn(new AuthUserVO(7L, "alice", "user"));
        when(aiConfigService.list(ConfigKind.MODEL)).thenReturn(List.of(
                new AiConfigRecordVO("model_deepseek", "DeepSeek", "model", "{\"temperature\":0.7}", "model-secret", "api_deepseek", 1, 0L, null, null, null, null),
                new AiConfigRecordVO("model_disabled", "Disabled", "model", "{}", "model-secret", "api_disabled", 0, 0L, null, null, null, null)));

        mockMvc.perform(get("/ai/models").header("Authorization", "Bearer token-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].configId").value("model_deepseek"))
                .andExpect(jsonPath("$.data[0].name").value("DeepSeek"))
                .andExpect(jsonPath("$.data[0].secret").doesNotExist())
                .andExpect(jsonPath("$.data[0].content").doesNotExist())
                .andExpect(jsonPath("$.data.length()").value(1));
    }
}
