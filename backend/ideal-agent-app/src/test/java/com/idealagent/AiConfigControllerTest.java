package com.idealagent;

import com.idealagent.domain.auth.model.vo.AuthUserVO;
import com.idealagent.domain.auth.service.ITokenParser;
import com.idealagent.domain.config.model.dto.AiConfigRecordDTO;
import com.idealagent.domain.config.model.vo.AiConfigRecordVO;
import com.idealagent.domain.config.service.AiConfigService;
import com.idealagent.domain.config.service.ConfigKind;
import com.idealagent.trigger.config.WebMvcConfig;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
}
