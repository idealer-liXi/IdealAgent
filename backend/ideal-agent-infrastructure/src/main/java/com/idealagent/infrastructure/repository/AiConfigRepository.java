package com.idealagent.infrastructure.repository;

import com.idealagent.domain.config.model.entity.AiConfigRecord;
import com.idealagent.domain.config.repository.IAiConfigRepository;
import com.idealagent.domain.config.service.ConfigKind;
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
        AiConfigData data = toData(record);
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

    private AiConfigData toData(AiConfigRecord record) {
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
        return data;
    }

    private AiConfigRecord toDomain(AiConfigData data) {
        AiConfigRecord record = new AiConfigRecord();
        record.setConfigId(data.getConfigId());
        record.setName(data.getName());
        record.setType(data.getType());
        record.setContent(data.getContent());
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
}
