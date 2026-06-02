package com.idealagent;

import com.idealagent.domain.user.model.vo.AuthUserVO;
import com.idealagent.domain.user.service.auth.ITokenParser;
import com.idealagent.domain.ai.model.dto.AiConfigRecordDTO;
import com.idealagent.domain.ai.model.vo.AiConfigRecordVO;
import com.idealagent.domain.ai.service.config.AiConfigService;
import com.idealagent.domain.ai.model.enumeration.ConfigKind;
import com.idealagent.config.WebMvcConfig;
import com.idealagent.trigger.controller.AiConfigController;
import com.idealagent.trigger.interceptor.AuthInterceptor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AiConfigController.class)
@Import({WebMvcConfig.class, AuthInterceptor.class})
class AiConfigControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AiConfigService aiConfigService;

    @MockitoBean
    private ITokenParser tokenParser;

    @Test
    void createApiConfigurationRequiresLoginAndReturnsRecord() throws Exception {
        when(tokenParser.parseToken("token-1")).thenReturn(new AuthUserVO(1L, "alice", "user"));
        when(aiConfigService.create(eq(ConfigKind.API), any(AiConfigRecordDTO.class)))
                .thenReturn(new AiConfigRecordVO("api_openai", "OpenAI", "openai", "https://api.openai.com", "sk-test", null, 1, 0L, null, null, null, null));

        mockMvc.perform(post("/ai/config/api")
                        .header("Authorization", "Bearer token-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"configId\":\"api_openai\",\"name\":\"OpenAI\",\"type\":\"openai\",\"content\":\"https://api.openai.com\",\"secret\":\"sk-test\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0000"))
                .andExpect(jsonPath("$.data.configId").value("api_openai"));
    }

    @Test
    void listModelConfigurationsReturnsRecords() throws Exception {
        when(tokenParser.parseToken("token-1")).thenReturn(new AuthUserVO(1L, "alice", "user"));
        when(aiConfigService.list(ConfigKind.MODEL))
                .thenReturn(List.of(new AiConfigRecordVO("model_gpt", "GPT", "chat", null, null, "api_openai", 1, 0L, null, null, null, null)));

        mockMvc.perform(get("/ai/config/model")
                        .header("Authorization", "Bearer token-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].configId").value("model_gpt"));
    }

    @Test
    void updateApiConfigurationReturnsRecord() throws Exception {
        when(tokenParser.parseToken("token-1")).thenReturn(new AuthUserVO(1L, "alice", "user"));
        when(aiConfigService.update(eq(ConfigKind.API), eq("api_deepseek"), any(AiConfigRecordDTO.class)))
                .thenReturn(new AiConfigRecordVO("api_deepseek", "DeepSeek", "openai", "https://api.deepseek.com", "sk-test", null, 1, 0L, null, null, null, null));

        mockMvc.perform(put("/ai/config/api/api_deepseek")
                        .header("Authorization", "Bearer token-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"DeepSeek\",\"type\":\"openai\",\"content\":\"https://api.deepseek.com\",\"secret\":\"sk-test\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.configId").value("api_deepseek"));
    }

    @Test
    void updateStatusAndDeleteConfiguration() throws Exception {
        when(tokenParser.parseToken("token-1")).thenReturn(new AuthUserVO(1L, "alice", "user"));

        mockMvc.perform(patch("/ai/config/client/client_chat/status")
                        .header("Authorization", "Bearer token-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0000"));

        mockMvc.perform(delete("/ai/config/client/client_chat")
                        .header("Authorization", "Bearer token-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0000"));
    }
}
