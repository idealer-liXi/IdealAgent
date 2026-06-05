package com.idealagent;

import com.idealagent.config.WebMvcConfig;
import com.idealagent.domain.ai.model.dto.FlowManageDTO;
import com.idealagent.domain.ai.model.vo.AgentManageVO;
import com.idealagent.domain.ai.model.vo.FlowManageVO;
import com.idealagent.domain.ai.service.agent.AgentManagementService;
import com.idealagent.domain.user.model.vo.AuthUserVO;
import com.idealagent.domain.user.service.auth.ITokenParser;
import com.idealagent.trigger.controller.AiAgentController;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AiAgentController.class)
@Import({WebMvcConfig.class, AuthInterceptor.class})
class AiAgentControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AgentManagementService agentManagementService;

    @MockitoBean
    private ITokenParser tokenParser;

    @Test
    void agentsReturnsManagedAgents() throws Exception {
        when(tokenParser.parseToken("token-1")).thenReturn(new AuthUserVO(7L, "alice", "user"));
        when(agentManagementService.listAgents()).thenReturn(List.of(new AgentManageVO(
                "agent_custom_step", "Custom Step", "step", "desc", "", "", 1)));

        mockMvc.perform(get("/ai/agents").header("Authorization", "Bearer token-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].agentId").value("agent_custom_step"));
    }

    @Test
    void createFlowDelegatesToService() throws Exception {
        when(tokenParser.parseToken("token-1")).thenReturn(new AuthUserVO(7L, "alice", "user"));
        when(agentManagementService.createFlow(any(FlowManageDTO.class))).thenReturn(new FlowManageVO(
                "flow_custom_planner", "agent_custom_step", "client_default_chat", "planner", 2, "prompt_step_planner", "Plan prompt", 1));

        mockMvc.perform(post("/ai/flows")
                        .header("Authorization", "Bearer token-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"flowId\":\"flow_custom_planner\",\"agentId\":\"agent_custom_step\",\"clientId\":\"client_default_chat\",\"roleType\":\"planner\",\"sortOrder\":2,\"promptId\":\"prompt_step_planner\",\"status\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.flowId").value("flow_custom_planner"));

        verify(agentManagementService).createFlow(any(FlowManageDTO.class));
    }
}
