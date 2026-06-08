package com.idealagent.trigger.controller;

import com.idealagent.domain.ai.model.dto.AgentManageDTO;
import com.idealagent.domain.ai.model.dto.CanvasNodeDTO;
import com.idealagent.domain.ai.model.dto.CanvasRelationDTO;
import com.idealagent.domain.ai.model.dto.CanvasSaveDTO;
import com.idealagent.domain.ai.model.dto.FlowManageDTO;
import com.idealagent.domain.ai.model.vo.AgentManageVO;
import com.idealagent.domain.ai.model.vo.CanvasGraphVO;
import com.idealagent.domain.ai.model.vo.FlowManageVO;
import com.idealagent.domain.ai.model.vo.FlowOptionsVO;
import com.idealagent.domain.ai.service.agent.AdminCanvasService;
import com.idealagent.domain.ai.service.agent.AgentManagementService;
import com.idealagent.trigger.context.UserContext;
import com.idealagent.types.result.Result;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/ai")
public class AiAgentController {
    private final AgentManagementService agentManagementService;
    private final AdminCanvasService adminCanvasService;

    public AiAgentController(AgentManagementService agentManagementService, AdminCanvasService adminCanvasService) {
        this.agentManagementService = agentManagementService;
        this.adminCanvasService = adminCanvasService;
    }

    @GetMapping("/agents")
    public Result<List<AgentManageVO>> agents() {
        return Result.success(agentManagementService.listAgents());
    }

    @GetMapping("/admin/agents")
    public Result<List<AgentManageVO>> adminAgents() {
        UserContext.requireAdmin();
        return Result.success(agentManagementService.listAgents());
    }

    @PostMapping("/agents")
    public Result<AgentManageVO> createAgent(@RequestBody AgentManageDTO request) {
        UserContext.requireAdmin();
        return Result.success(agentManagementService.createAgent(request));
    }

    @PostMapping("/admin/agents")
    public Result<AgentManageVO> adminCreateAgent(@RequestBody AgentManageDTO request) {
        UserContext.requireAdmin();
        return Result.success(agentManagementService.createAgent(request));
    }

    @PutMapping("/agents/{agentId}")
    public Result<AgentManageVO> updateAgent(@PathVariable String agentId, @RequestBody AgentManageDTO request) {
        UserContext.requireAdmin();
        return Result.success(agentManagementService.updateAgent(agentId, request));
    }

    @PutMapping("/admin/agents/{agentId}")
    public Result<AgentManageVO> adminUpdateAgent(@PathVariable String agentId, @RequestBody AgentManageDTO request) {
        UserContext.requireAdmin();
        return Result.success(agentManagementService.updateAgent(agentId, request));
    }

    @PatchMapping("/agents/{agentId}/status")
    public Result<Void> updateAgentStatus(@PathVariable String agentId, @RequestBody AgentManageDTO request) {
        UserContext.requireAdmin();
        agentManagementService.updateAgentStatus(agentId, request.status());
        return Result.success(null);
    }

    @PatchMapping("/admin/agents/{agentId}/status")
    public Result<Void> adminUpdateAgentStatus(@PathVariable String agentId, @RequestBody AgentManageDTO request) {
        UserContext.requireAdmin();
        agentManagementService.updateAgentStatus(agentId, request.status());
        return Result.success(null);
    }

    @DeleteMapping("/agents/{agentId}")
    public Result<Void> deleteAgent(@PathVariable String agentId) {
        UserContext.requireAdmin();
        agentManagementService.deleteAgent(agentId);
        return Result.success(null);
    }

    @DeleteMapping("/admin/agents/{agentId}")
    public Result<Void> adminDeleteAgent(@PathVariable String agentId) {
        UserContext.requireAdmin();
        agentManagementService.deleteAgent(agentId);
        return Result.success(null);
    }

    @GetMapping("/admin/flows/agents")
    public Result<List<AgentManageVO>> adminFlowAgents() {
        UserContext.requireAdmin();
        return Result.success(agentManagementService.listAgents());
    }

    @GetMapping("/agents/{agentId}/flows")
    public Result<List<FlowManageVO>> flows(@PathVariable String agentId) {
        UserContext.requireAdmin();
        return Result.success(agentManagementService.listFlows(agentId));
    }

    @GetMapping("/admin/flows/{agentId}")
    public Result<List<FlowManageVO>> adminFlows(@PathVariable String agentId) {
        UserContext.requireAdmin();
        return Result.success(agentManagementService.listFlows(agentId));
    }

    @PostMapping("/flows")
    public Result<FlowManageVO> createFlow(@RequestBody FlowManageDTO request) {
        UserContext.requireAdmin();
        return Result.success(agentManagementService.createFlow(request));
    }

    @PostMapping("/admin/flows")
    public Result<FlowManageVO> adminCreateFlow(@RequestBody FlowManageDTO request) {
        UserContext.requireAdmin();
        return Result.success(agentManagementService.createFlow(request));
    }

    @PutMapping("/admin/flows")
    public Result<FlowManageVO> adminUpdateFlow(@RequestBody FlowManageDTO request) {
        UserContext.requireAdmin();
        return Result.success(agentManagementService.updateFlow(request));
    }

    @DeleteMapping("/admin/flows")
    public Result<Void> adminDeleteFlow(@RequestParam String agentId, @RequestParam String clientId) {
        UserContext.requireAdmin();
        agentManagementService.deleteFlow(agentId, clientId);
        return Result.success(null);
    }

    @GetMapping("/admin/canvas/{agentId}")
    public Result<CanvasGraphVO> adminCanvas(@PathVariable String agentId) {
        UserContext.requireAdmin();
        return Result.success(adminCanvasService.graph(agentId));
    }

    @PostMapping("/admin/canvas/{agentId}/save")
    public Result<Void> adminSaveCanvas(@PathVariable String agentId, @RequestBody CanvasSaveDTO request) {
        UserContext.requireAdmin();
        adminCanvasService.saveCanvas(agentId, request);
        return Result.success(null);
    }

    @PostMapping("/admin/canvas/node")
    public Result<Void> adminCreateCanvasNode(@RequestBody CanvasNodeDTO request) {
        UserContext.requireAdmin();
        adminCanvasService.saveNode(request);
        return Result.success(null);
    }

    @PutMapping("/admin/canvas/node")
    public Result<Void> adminUpdateCanvasNode(@RequestBody CanvasNodeDTO request) {
        UserContext.requireAdmin();
        adminCanvasService.saveNode(request);
        return Result.success(null);
    }

    @PostMapping("/admin/canvas/relation")
    public Result<Void> adminCreateCanvasRelation(@RequestBody CanvasRelationDTO request) {
        UserContext.requireAdmin();
        adminCanvasService.saveRelation(request);
        return Result.success(null);
    }

    @DeleteMapping("/admin/canvas/relation")
    public Result<Void> adminDeleteCanvasRelation(@RequestBody CanvasRelationDTO request) {
        UserContext.requireAdmin();
        adminCanvasService.deleteRelation(request);
        return Result.success(null);
    }

    @GetMapping("/flow-options")
    public Result<FlowOptionsVO> flowOptions() {
        UserContext.requireAdmin();
        return Result.success(agentManagementService.options());
    }
}
