package com.idealagent.trigger.controller;

import com.idealagent.domain.config.model.dto.AiConfigRecordDTO;
import com.idealagent.domain.config.model.vo.AiConfigRecordVO;
import com.idealagent.domain.config.service.AiConfigService;
import com.idealagent.domain.config.service.ConfigKind;
import com.idealagent.types.result.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
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

    @PutMapping("/{kind}/{configId}")
    public Result<AiConfigRecordVO> update(@PathVariable String kind, @PathVariable String configId, @RequestBody AiConfigRecordDTO request) {
        return Result.success(aiConfigService.update(ConfigKind.from(kind), configId, request));
    }

    @PatchMapping("/{kind}/{configId}/status")
    public Result<Void> updateStatus(@PathVariable String kind, @PathVariable String configId, @RequestBody AiConfigRecordDTO request) {
        aiConfigService.updateStatus(ConfigKind.from(kind), configId, request.status());
        return Result.success(null);
    }

    @DeleteMapping("/{kind}/{configId}")
    public Result<Void> delete(@PathVariable String kind, @PathVariable String configId) {
        aiConfigService.delete(ConfigKind.from(kind), configId);
        return Result.success(null);
    }
}
