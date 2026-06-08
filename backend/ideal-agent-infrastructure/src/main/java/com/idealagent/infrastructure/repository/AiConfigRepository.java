package com.idealagent.infrastructure.repository;

import com.idealagent.domain.ai.model.entity.AiConfigRecord;
import com.idealagent.domain.ai.model.entity.McpServerConfig;
import com.idealagent.domain.ai.repository.IAiConfigRepository;
import com.idealagent.domain.ai.model.enumeration.ConfigKind;
import com.idealagent.infrastructure.persistent.dao.IAiConfigDao;
import com.idealagent.infrastructure.persistent.po.AiConfigData;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class AiConfigRepository implements IAiConfigRepository {
    private final IAiConfigDao aiConfigDao;

    public AiConfigRepository(IAiConfigDao aiConfigDao) {
        this.aiConfigDao = aiConfigDao;
    }

    @Override
    public AiConfigRecord save(ConfigKind kind, AiConfigRecord record) {
        AiConfigData data = toData(kind, record);
        switch (kind) {
            case API -> aiConfigDao.insertApi(data);
            case MODEL -> aiConfigDao.insertModel(data);
            case CLIENT -> aiConfigDao.insertClient(data);
            case PROMPT -> aiConfigDao.insertPrompt(data);
            case ADVISOR -> aiConfigDao.insertAdvisor(data);
            case MCP -> aiConfigDao.insertMcp(data);
            case CONFIG -> aiConfigDao.insertConfig(data);
        }
        return record;
    }

    @Override
    public List<AiConfigRecord> list(ConfigKind kind) {
        List<AiConfigData> records = switch (kind) {
            case API -> aiConfigDao.listApis();
            case MODEL -> aiConfigDao.listModels();
            case CLIENT -> aiConfigDao.listClients();
            case PROMPT -> aiConfigDao.listPrompts();
            case ADVISOR -> aiConfigDao.listAdvisors();
            case MCP -> aiConfigDao.listMcps();
            case CONFIG -> aiConfigDao.listConfigs();
        };
        return records.stream().map(this::toDomain).toList();
    }

    @Override
    public AiConfigRecord find(ConfigKind kind, String configId) {
        return IAiConfigRepository.super.find(kind, configId);
    }

    @Override
    public AiConfigRecord update(ConfigKind kind, AiConfigRecord record) {
        AiConfigData data = toData(kind, record);
        switch (kind) {
            case API -> aiConfigDao.updateApi(data);
            case MODEL -> aiConfigDao.updateModel(data);
            case CLIENT -> aiConfigDao.updateClient(data);
            case PROMPT -> aiConfigDao.updatePrompt(data);
            case ADVISOR -> aiConfigDao.updateAdvisor(data);
            case MCP -> aiConfigDao.updateMcp(data);
            case CONFIG -> aiConfigDao.updateConfig(data);
        }
        return record;
    }

    @Override
    public void updateStatus(ConfigKind kind, String configId, Integer status) {
        switch (kind) {
            case API -> aiConfigDao.updateApiStatus(configId, status);
            case MODEL -> aiConfigDao.updateModelStatus(configId, status);
            case CLIENT -> aiConfigDao.updateClientStatus(configId, status);
            case PROMPT -> aiConfigDao.updatePromptStatus(configId, status);
            case ADVISOR -> aiConfigDao.updateAdvisorStatus(configId, status);
            case MCP -> aiConfigDao.updateMcpStatus(configId, status);
            case CONFIG -> aiConfigDao.updateConfigStatus(configId, status);
        }
    }

    @Override
    public void delete(ConfigKind kind, String configId) {
        switch (kind) {
            case API -> aiConfigDao.deleteApi(configId);
            case MODEL -> aiConfigDao.deleteModel(configId);
            case CLIENT -> aiConfigDao.deleteClient(configId);
            case PROMPT -> aiConfigDao.deletePrompt(configId);
            case ADVISOR -> aiConfigDao.deleteAdvisor(configId);
            case MCP -> aiConfigDao.deleteMcp(configId);
            case CONFIG -> aiConfigDao.deleteConfig(configId);
        }
    }

    private AiConfigData toData(ConfigKind kind, AiConfigRecord record) {
        AiConfigData data = new AiConfigData();
        data.setConfigId(record.getConfigId());
        data.setName(record.getName());
        data.setType(record.getType());
        data.setContent(record.getContent());
        data.setSecret(record.getSecret());
        data.setRefId(record.getRefId());
        data.setStatus(record.getStatus());
        data.setOwnerId(record.getOwnerId());
        data.setOwnerType(record.getOwnerType());
        data.setConfigType(record.getConfigType());
        if (kind == ConfigKind.MCP) {
            data.setTimeoutMinutes(Math.toIntExact(McpServerConfig.parse(record.getContent()).timeoutMinutes()));
        }
        return data;
    }

    private AiConfigRecord toDomain(AiConfigData data) {
        AiConfigRecord record = new AiConfigRecord();
        record.setConfigId(data.getConfigId());
        record.setName(data.getName());
        record.setType(data.getType());
        record.setContent(withMcpTimeout(data.getContent(), data.getTimeoutMinutes()));
        record.setSecret(data.getSecret());
        record.setRefId(data.getRefId());
        record.setStatus(data.getStatus());
        record.setOwnerId(data.getOwnerId());
        record.setOwnerType(data.getOwnerType());
        record.setConfigType(data.getConfigType());
        record.setCreateTime(data.getCreateTime());
        record.setUpdateTime(data.getUpdateTime());
        return record;
    }

    private String withMcpTimeout(String content, Integer timeoutMinutes) {
        if (timeoutMinutes == null) {
            return content;
        }
        McpServerConfig.parse(content);
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            java.util.Map<String, Object> map = mapper.readValue(content == null || content.isBlank() ? "{}" : content, new com.fasterxml.jackson.core.type.TypeReference<>() {
            });
            map.put("timeoutMinutes", timeoutMinutes);
            return mapper.writeValueAsString(map);
        } catch (Exception ignored) {
            return content == null || content.isBlank() ? "{\"timeoutMinutes\":" + timeoutMinutes + "}" : content;
        }
    }
}
