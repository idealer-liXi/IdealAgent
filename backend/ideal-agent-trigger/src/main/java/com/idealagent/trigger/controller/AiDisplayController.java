package com.idealagent.trigger.controller;

import com.idealagent.domain.ai.model.enumeration.ConfigKind;
import com.idealagent.domain.ai.model.vo.AiConfigRecordVO;
import com.idealagent.domain.ai.service.config.AiConfigService;
import com.idealagent.types.result.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/ai")
public class AiDisplayController {
    private final AiConfigService aiConfigService;

    public AiDisplayController(AiConfigService aiConfigService) {
        this.aiConfigService = aiConfigService;
    }

    @GetMapping("/mcps")
    public Result<List<McpDisplayVO>> mcps() {
        return Result.success(aiConfigService.list(ConfigKind.MCP).stream()
                .filter(record -> Integer.valueOf(1).equals(record.status()))
                .map(McpDisplayVO::from)
                .toList());
    }

    @GetMapping("/models")
    public Result<List<ConfigDisplayVO>> models() {
        return Result.success(aiConfigService.list(ConfigKind.MODEL).stream()
                .filter(record -> Integer.valueOf(1).equals(record.status()))
                .map(ConfigDisplayVO::from)
                .toList());
    }

    public record McpDisplayVO(String configId, String name, String type, Integer status) {
        private static McpDisplayVO from(AiConfigRecordVO record) {
            return new McpDisplayVO(record.configId(), record.name(), record.type(), record.status());
        }
    }

    public record ConfigDisplayVO(String configId, String name, String type, Integer status) {
        private static ConfigDisplayVO from(AiConfigRecordVO record) {
            return new ConfigDisplayVO(record.configId(), record.name(), record.type(), record.status());
        }
    }
}
