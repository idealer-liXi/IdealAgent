package com.idealagent;

import com.idealagent.config.WebMvcConfig;
import com.idealagent.domain.ai.model.dto.CanvasRelationDTO;
import com.idealagent.domain.ai.model.dto.FlowManageDTO;
import com.idealagent.domain.ai.model.vo.AgentManageVO;
import com.idealagent.domain.ai.model.vo.CanvasGraphVO;
import com.idealagent.domain.ai.model.vo.FlowManageVO;
import com.idealagent.domain.ai.service.agent.AdminCanvasService;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
    private AdminCanvasService adminCanvasService;

    @MockitoBean
    private ITokenParser tokenParser;

    @Test
    void agentsReturnsManagedAgents() throws Exception {
        when(tokenParser.parseToken("token-1")).thenReturn(new AuthUserVO(7L, "alice", "admin"));
        when(agentManagementService.listAgents()).thenReturn(List.of(new AgentManageVO(
                "agent_custom_step", "Custom Step", "step", "desc", "", "", 1)));

        mockMvc.perform(get("/ai/admin/agents").header("Authorization", "Bearer token-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].agentId").value("agent_custom_step"));
    }

    @Test
    void createsAdminFlow() throws Exception {
        when(tokenParser.parseToken("token-1")).thenReturn(new AuthUserVO(7L, "alice", "admin"));
        when(agentManagementService.createFlow(any(FlowManageDTO.class))).thenReturn(new FlowManageVO(
                "agent_custom_step", "client_planner", "planner", "Plan %s", 2));

        mockMvc.perform(post("/ai/admin/flows")
                        .header("Authorization", "Bearer token-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"agentId\":\"agent_custom_step\",\"clientId\":\"client_planner\",\"clientRole\":\"planner\",\"userPrompt\":\"Plan %s\",\"flowSeq\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.clientRole").value("planner"));

        verify(agentManagementService).createFlow(any(FlowManageDTO.class));
    }

    @Test
    void deletesAdminFlowByCompositeKey() throws Exception {
        when(tokenParser.parseToken("token-1")).thenReturn(new AuthUserVO(7L, "alice", "admin"));

        mockMvc.perform(delete("/ai/admin/flows")
                        .header("Authorization", "Bearer token-1")
                        .param("agentId", "agent_custom_step")
                        .param("clientId", "client_planner"))
                .andExpect(status().isOk());

        verify(agentManagementService).deleteFlow("agent_custom_step", "client_planner");
    }

    @Test
    void returnsAdminCanvasGraph() throws Exception {
        when(tokenParser.parseToken("token-1")).thenReturn(new AuthUserVO(7L, "alice", "admin"));
        when(adminCanvasService.graph("agent_custom_step")).thenReturn(new CanvasGraphVO(
                new AgentManageVO("agent_custom_step", "Custom Step", "step", "desc", "", "", 1),
                List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of()));

        mockMvc.perform(get("/ai/admin/canvas/agent_custom_step")
                        .header("Authorization", "Bearer token-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.agent.agentId").value("agent_custom_step"));
    }

    @Test
    void savesAdminCanvasRelation() throws Exception {
        when(tokenParser.parseToken("token-1")).thenReturn(new AuthUserVO(7L, "alice", "admin"));

        mockMvc.perform(post("/ai/admin/canvas/relation")
                        .header("Authorization", "Bearer token-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sourceType\":\"client\",\"targetType\":\"prompt\",\"sourceId\":\"client_planner\",\"targetId\":\"prompt_system\",\"agentId\":\"agent_custom_step\",\"configType\":\"prompt\"}"))
                .andExpect(status().isOk());

        verify(adminCanvasService).saveRelation(any(CanvasRelationDTO.class));
    }

    @Test
    void publicAgentsListIsVisibleToNormalUsers() throws Exception {
        when(tokenParser.parseToken("token-1")).thenReturn(new AuthUserVO(7L, "alice", "user"));
        when(agentManagementService.listAgents()).thenReturn(List.of(new AgentManageVO(
                "agent_custom_step", "Custom Step", "step", "desc", "", "", 1)));

        mockMvc.perform(get("/ai/agents").header("Authorization", "Bearer token-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].agentId").value("agent_custom_step"));
    }

    @Test
    void rejectsNonAdminAgentManagementAccess() throws Exception {
        when(tokenParser.parseToken("token-1")).thenReturn(new AuthUserVO(7L, "alice", "user"));

        mockMvc.perform(get("/ai/admin/agents").header("Authorization", "Bearer token-1"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("0001"));
    }

    @Test
    void rejectsNonAdminLegacyFlowAccess() throws Exception {
        when(tokenParser.parseToken("token-1")).thenReturn(new AuthUserVO(7L, "alice", "user"));

        mockMvc.perform(get("/ai/agents/agent_custom_step/flows").header("Authorization", "Bearer token-1"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("0001"));
    }
}
