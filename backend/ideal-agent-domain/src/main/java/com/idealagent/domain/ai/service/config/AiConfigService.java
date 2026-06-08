package com.idealagent.domain.ai.service.config;

import com.idealagent.domain.ai.model.dto.AiConfigRecordDTO;
import com.idealagent.domain.ai.model.entity.AiConfigRecord;
import com.idealagent.domain.ai.model.enumeration.ConfigKind;
import com.idealagent.domain.ai.model.vo.AiConfigRecordVO;
import com.idealagent.domain.ai.repository.IAiConfigRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class AiConfigService {
    private static final int ENABLED = 1;
    private static final String MODEL_TYPE = "model";
    private static final String CHAT_CLIENT_TYPE = "chat";
    private static final String WORK_CLIENT_TYPE = "work";

    private final IAiConfigRepository aiConfigRepository;

    public AiConfigService(IAiConfigRepository aiConfigRepository) {
        this.aiConfigRepository = aiConfigRepository;
    }

    public AiConfigRecordVO create(ConfigKind kind, AiConfigRecordDTO request) {
        validate(request);
        AiConfigRecord record = new AiConfigRecord();
        record.setConfigId(request.configId());
        record.setName(request.name());
        record.setType(normalizeType(kind, request.type()));
        record.setContent(request.content());
        record.setSecret(request.secret());
        record.setRefId(request.refId());
        record.setStatus(request.status() == null ? ENABLED : request.status());
        record.setOwnerId(request.ownerId() == null ? 0L : request.ownerId());
        record.setOwnerType(request.ownerType());
        record.setConfigType(request.configType());
        validateBindingBoundary(kind, record);
        return toVo(aiConfigRepository.save(kind, record));
    }

    public List<AiConfigRecordVO> list(ConfigKind kind) {
        return aiConfigRepository.list(kind).stream().map(this::toVo).toList();
    }

    public AiConfigRecordVO update(ConfigKind kind, String configId, AiConfigRecordDTO request) {
        if (!StringUtils.hasText(configId)) {
            throw new AiConfigException("配置ID不能为空");
        }
        AiConfigRecord record = toRecord(kind, request, configId);
        validate(record);
        validateBindingBoundary(kind, record);
        return toVo(aiConfigRepository.update(kind, record));
    }

    public void updateStatus(ConfigKind kind, String configId, Integer status) {
        if (!StringUtils.hasText(configId)) {
            throw new AiConfigException("配置ID不能为空");
        }
        if (status == null || (status != 0 && status != 1)) {
            throw new AiConfigException("配置状态不正确");
        }
        aiConfigRepository.updateStatus(kind, configId, status);
    }

    public void delete(ConfigKind kind, String configId) {
        if (!StringUtils.hasText(configId)) {
            throw new AiConfigException("配置ID不能为空");
        }
        assertNotReferenced(kind, configId);
        aiConfigRepository.delete(kind, configId);
    }

    private void assertNotReferenced(ConfigKind kind, String configId) {
        switch (kind) {
            case API -> assertNoRef(ConfigKind.MODEL, configId, "API 已被 Model 引用，不能删除");
            case MODEL -> assertNoRef(ConfigKind.CLIENT, configId, "Model 已被 Client 引用，不能删除");
            case CLIENT -> assertNoBindingOwner(configId);
            case PROMPT -> assertNoBindingTarget("prompt", configId, "Prompt 已被 Binding 引用，不能删除");
            case ADVISOR -> assertNoBindingTarget("advisor", configId, "Advisor 已被 Binding 引用，不能删除");
            case MCP -> assertNoBindingTarget("mcp", configId, "MCP 已被 Binding 引用，不能删除");
            case CONFIG -> {
            }
        }
    }

    private void assertNoRef(ConfigKind kind, String refId, String message) {
        boolean referenced = aiConfigRepository.list(kind).stream()
                .anyMatch(record -> refId.equals(record.getRefId()));
        if (referenced) {
            throw new AiConfigException(message);
        }
    }

    private void assertNoBindingOwner(String ownerId) {
        boolean referenced = aiConfigRepository.list(ConfigKind.CONFIG).stream()
                .anyMatch(record -> ownerId.equals(record.getContent()) || ownerId.equals(String.valueOf(record.getOwnerId())));
        if (referenced) {
            throw new AiConfigException("Client 已被 Binding 引用，不能删除");
        }
    }

    private void assertNoBindingTarget(String configType, String refId, String message) {
        boolean referenced = aiConfigRepository.list(ConfigKind.CONFIG).stream()
                .anyMatch(record -> configType.equals(record.getConfigType()) && refId.equals(record.getRefId()));
        if (referenced) {
            throw new AiConfigException(message);
        }
    }

    private void validate(AiConfigRecordDTO request) {
        if (!StringUtils.hasText(request.configId())) {
            throw new AiConfigException("配置ID不能为空");
        }
        if (!StringUtils.hasText(request.name()) && !StringUtils.hasText(request.content())) {
            throw new AiConfigException("配置名称不能为空");
        }
    }

    private void validate(AiConfigRecord record) {
        if (!StringUtils.hasText(record.getConfigId())) {
            throw new AiConfigException("配置ID不能为空");
        }
        if (!StringUtils.hasText(record.getName()) && !StringUtils.hasText(record.getContent())) {
            throw new AiConfigException("配置名称不能为空");
        }
    }

    private AiConfigRecord toRecord(ConfigKind kind, AiConfigRecordDTO request, String configId) {
        AiConfigRecord record = new AiConfigRecord();
        record.setConfigId(configId);
        record.setName(request.name());
        record.setType(normalizeType(kind, request.type()));
        record.setContent(request.content());
        record.setSecret(request.secret());
        record.setRefId(request.refId());
        record.setStatus(request.status() == null ? ENABLED : request.status());
        record.setOwnerId(request.ownerId() == null ? 0L : request.ownerId());
        record.setOwnerType(request.ownerType());
        record.setConfigType(request.configType());
        return record;
    }

    private String normalizeType(ConfigKind kind, String type) {
        if (kind == ConfigKind.MODEL) {
            return MODEL_TYPE;
        }
        if (kind == ConfigKind.CLIENT) {
            if (WORK_CLIENT_TYPE.equalsIgnoreCase(type)) {
                return WORK_CLIENT_TYPE;
            }
            return CHAT_CLIENT_TYPE;
        }
        return type;
    }

    private void validateBindingBoundary(ConfigKind kind, AiConfigRecord record) {
        if (kind != ConfigKind.CONFIG || !"client".equalsIgnoreCase(firstText(record.getOwnerType(), record.getType()))) {
            return;
        }
        String clientId = firstText(record.getContent(), String.valueOf(record.getOwnerId()));
        AiConfigRecord client = aiConfigRepository.find(ConfigKind.CLIENT, clientId);
        if (client == null || !CHAT_CLIENT_TYPE.equalsIgnoreCase(client.getType())) {
            return;
        }
        String configType = firstText(record.getConfigType(), record.getSecret());
        if ("mcp".equalsIgnoreCase(configType)) {
            throw new AiConfigException("Chat Client 不允许绑定 MCP");
        }
        if ("advisor".equalsIgnoreCase(configType)) {
            AiConfigRecord advisor = aiConfigRepository.find(ConfigKind.ADVISOR, record.getRefId());
            if (advisor != null && "rag".equalsIgnoreCase(advisor.getType())) {
                throw new AiConfigException("Chat Client 不允许绑定 RAG Advisor");
            }
        }
    }

    private String firstText(String first, String second) {
        return StringUtils.hasText(first) ? first : second;
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
