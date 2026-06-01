package com.idealagent.trigger.controller;

import com.idealagent.domain.config.model.dto.AiConfigRecordDTO;
import com.idealagent.domain.config.model.vo.AiConfigRecordVO;
import com.idealagent.domain.config.service.AiConfigService;
import com.idealagent.domain.config.service.ConfigKind;
import com.idealagent.types.result.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/ai/config")
public class AiConfigController {
    private final AiConfigService aiConfigService;

    public AiConfigController(AiConfigService aiConfigService) {
        this.aiConfigService = aiConfigService;
    }

    @PostMapping("/{kind}")
    public Result<AiConfigRecordVO> create(@PathVariable String kind, @RequestBody AiConfigRecordDTO request) {
        return Result.success(aiConfigService.create(ConfigKind.from(kind), request));
    }

    @GetMapping("/{kind}")
    public Result<List<AiConfigRecordVO>> list(@PathVariable String kind) {
        return Result.success(aiConfigService.list(ConfigKind.from(kind)));
    }
}
