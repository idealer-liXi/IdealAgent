package com.idealagent.trigger.controller;

import com.idealagent.domain.ai.model.dto.WorkshopAgentCreateDTO;
import com.idealagent.domain.ai.model.vo.AgentManageVO;
import com.idealagent.domain.ai.service.workshop.WorkshopService;
import com.idealagent.types.result.Result;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ai/workshop")
public class AiWorkshopController {
    private final WorkshopService workshopService;

    public AiWorkshopController(WorkshopService workshopService) {
        this.workshopService = workshopService;
    }

    @PostMapping("/agents")
    public Result<AgentManageVO> createAgent(@RequestBody WorkshopAgentCreateDTO request) {
        return Result.success(workshopService.createAgent(request));
    }
}
