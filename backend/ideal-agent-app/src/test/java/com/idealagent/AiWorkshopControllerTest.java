package com.idealagent;

import com.idealagent.config.WebMvcConfig;
import com.idealagent.domain.ai.model.dto.WorkshopAgentCreateDTO;
import com.idealagent.domain.ai.model.vo.AgentManageVO;
import com.idealagent.domain.ai.service.workshop.WorkshopService;
import com.idealagent.domain.user.model.vo.AuthUserVO;
import com.idealagent.domain.user.service.auth.ITokenParser;
import com.idealagent.trigger.controller.AiWorkshopController;
import com.idealagent.trigger.interceptor.AuthInterceptor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AiWorkshopController.class)
@Import({WebMvcConfig.class, AuthInterceptor.class})
class AiWorkshopControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WorkshopService workshopService;

    @MockitoBean
    private ITokenParser tokenParser;

    @Test
    void normalUserCanCreateWorkshopAgent() throws Exception {
        when(tokenParser.parseToken("token-1")).thenReturn(new AuthUserVO(7L, "alice", "user"));
        when(workshopService.createAgent(any(WorkshopAgentCreateDTO.class))).thenReturn(new AgentManageVO(
                "agent_news", "News Agent", "step", "desc", "model_deepseek", "", 1));

        mockMvc.perform(post("/ai/workshop/agents")
                        .header("Authorization", "Bearer token-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"agentName\":\"News Agent\",\"agentDesc\":\"desc\",\"strategy\":\"step\",\"modelId\":\"model_deepseek\",\"mcpIdList\":[\"mcp_mail\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.agentId").value("agent_news"));
    }
}
