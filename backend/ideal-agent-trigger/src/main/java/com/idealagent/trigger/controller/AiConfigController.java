package com.idealagent.trigger.controller;

import com.idealagent.api.IAiConfigApi;
import com.idealagent.domain.ai.model.dto.AiConfigRecordDTO;
import com.idealagent.domain.ai.model.vo.AiConfigRecordVO;
import com.idealagent.domain.ai.service.config.AiConfigService;
import com.idealagent.domain.ai.model.enumeration.ConfigKind;
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
public class AiConfigController implements IAiConfigApi {
    private final AiConfigService aiConfigService;

    public AiConfigController(AiConfigService aiConfigService) {
        this.aiConfigService = aiConfigService;
    }

    @PostMapping("/{kind}")
    @Override
    public Result<AiConfigRecordVO> create(@PathVariable String kind, @RequestBody AiConfigRecordDTO request) {
        return Result.success(aiConfigService.create(ConfigKind.from(kind), request));
    }

    @GetMapping("/{kind}")
    @Override
    public Result<List<AiConfigRecordVO>> list(@PathVariable String kind) {
        return Result.success(aiConfigService.list(ConfigKind.from(kind)));
    }

    @PutMapping("/{kind}/{configId}")
    @Override
    public Result<AiConfigRecordVO> update(@PathVariable String kind, @PathVariable String configId, @RequestBody AiConfigRecordDTO request) {
        return Result.success(aiConfigService.update(ConfigKind.from(kind), configId, request));
    }

    @PatchMapping("/{kind}/{configId}/status")
    @Override
    public Result<Void> updateStatus(@PathVariable String kind, @PathVariable String configId, @RequestBody AiConfigRecordDTO request) {
        aiConfigService.updateStatus(ConfigKind.from(kind), configId, request.status());
        return Result.success(null);
    }

    @DeleteMapping("/{kind}/{configId}")
    @Override
    public Result<Void> delete(@PathVariable String kind, @PathVariable String configId) {
        aiConfigService.delete(ConfigKind.from(kind), configId);
        return Result.success(null);
    }
}
