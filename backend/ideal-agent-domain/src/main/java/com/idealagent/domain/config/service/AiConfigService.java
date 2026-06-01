package com.idealagent.domain.config.service;

import com.idealagent.domain.config.model.dto.AiConfigRecordDTO;
import com.idealagent.domain.config.model.entity.AiConfigRecord;
import com.idealagent.domain.config.model.vo.AiConfigRecordVO;
import com.idealagent.domain.config.repository.IAiConfigRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class AiConfigService {
    private static final int ENABLED = 1;

    private final IAiConfigRepository aiConfigRepository;

    public AiConfigService(IAiConfigRepository aiConfigRepository) {
        this.aiConfigRepository = aiConfigRepository;
    }

    public AiConfigRecordVO create(ConfigKind kind, AiConfigRecordDTO request) {
        validate(request);
        AiConfigRecord record = new AiConfigRecord();
        record.setConfigId(request.configId());
        record.setName(request.name());
        record.setType(request.type());
        record.setContent(request.content());
        record.setSecret(request.secret());
        record.setRefId(request.refId());
        record.setStatus(request.status() == null ? ENABLED : request.status());
        record.setOwnerId(request.ownerId() == null ? 0L : request.ownerId());
        record.setOwnerType(request.ownerType());
        record.setConfigType(request.configType());
        return toVo(aiConfigRepository.save(kind, record));
    }

    public List<AiConfigRecordVO> list(ConfigKind kind) {
        return aiConfigRepository.list(kind).stream().map(this::toVo).toList();
    }

    private void validate(AiConfigRecordDTO request) {
        if (!StringUtils.hasText(request.configId())) {
            throw new AiConfigException("配置ID不能为空");
        }
        if (!StringUtils.hasText(request.name()) && !StringUtils.hasText(request.content())) {
            throw new AiConfigException("配置名称不能为空");
        }
    }

    private AiConfigRecordVO toVo(AiConfigRecord record) {
        return new AiConfigRecordVO(
                record.getConfigId(),
                record.getName(),
                record.getType(),
                record.getContent(),
                record.getSecret(),
                record.getRefId(),
                record.getStatus(),
                record.getOwnerId(),
                record.getOwnerType(),
                record.getConfigType(),
                record.getCreateTime(),
                record.getUpdateTime());
    }
}
