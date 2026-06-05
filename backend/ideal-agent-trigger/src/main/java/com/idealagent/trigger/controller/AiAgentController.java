package com.idealagent.trigger.controller;

import com.idealagent.domain.ai.model.dto.AgentManageDTO;
import com.idealagent.domain.ai.model.dto.FlowManageDTO;
import com.idealagent.domain.ai.model.vo.AgentManageVO;
import com.idealagent.domain.ai.model.vo.FlowManageVO;
import com.idealagent.domain.ai.model.vo.FlowOptionsVO;
import com.idealagent.domain.ai.service.agent.AgentManagementService;
import com.idealagent.types.result.Result;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/ai")
public class AiAgentController {
    private final AgentManagementService agentManagementService;

    public AiAgentController(AgentManagementService agentManagementService) {
        this.agentManagementService = agentManagementService;
    }

    @GetMapping("/agents")
    public Result<List<AgentManageVO>> agents() {
        return Result.success(agentManagementService.listAgents());
    }

    @PostMapping("/agents")
    public Result<AgentManageVO> createAgent(@RequestBody AgentManageDTO request) {
        return Result.success(agentManagementService.createAgent(request));
    }

    @PutMapping("/agents/{agentId}")
    public Result<AgentManageVO> updateAgent(@PathVariable String agentId, @RequestBody AgentManageDTO request) {
        return Result.success(agentManagementService.updateAgent(agentId, request));
    }

    @PatchMapping("/agents/{agentId}/status")
    public Result<Void> updateAgentStatus(@PathVariable String agentId, @RequestBody AgentManageDTO request) {
        agentManagementService.updateAgentStatus(agentId, request.status());
        return Result.success(null);
    }

    @DeleteMapping("/agents/{agentId}")
    public Result<Void> deleteAgent(@PathVariable String agentId) {
        agentManagementService.deleteAgent(agentId);
        return Result.success(null);
    }

    @GetMapping("/agents/{agentId}/flows")
    public Result<List<FlowManageVO>> flows(@PathVariable String agentId) {
        return Result.success(agentManagementService.listFlows(agentId));
    }

    @PostMapping("/flows")
    public Result<FlowManageVO> createFlow(@RequestBody FlowManageDTO request) {
        return Result.success(agentManagementService.createFlow(request));
    }

    @PutMapping("/flows/{flowId}")
    public Result<FlowManageVO> updateFlow(@PathVariable String flowId, @RequestBody FlowManageDTO request) {
        return Result.success(agentManagementService.updateFlow(flowId, request));
    }

    @PatchMapping("/flows/{flowId}/status")
    public Result<Void> updateFlowStatus(@PathVariable String flowId, @RequestBody FlowManageDTO request) {
        agentManagementService.updateFlowStatus(flowId, request.status());
        return Result.success(null);
    }

    @DeleteMapping("/flows/{flowId}")
    public Result<Void> deleteFlow(@PathVariable String flowId) {
        agentManagementService.deleteFlow(flowId);
        return Result.success(null);
    }

    @GetMapping("/flow-options")
    public Result<FlowOptionsVO> flowOptions() {
        return Result.success(agentManagementService.options());
    }
}
