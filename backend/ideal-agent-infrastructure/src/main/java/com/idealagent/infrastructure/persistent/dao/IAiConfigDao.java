package com.idealagent.infrastructure.persistent.dao;

import com.idealagent.infrastructure.persistent.po.AiConfigData;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface IAiConfigDao {
    int insertApi(AiConfigData record);

    List<AiConfigData> listApis();

    int insertModel(AiConfigData record);

    List<AiConfigData> listModels();

    int insertClient(AiConfigData record);

    List<AiConfigData> listClients();

    int insertPrompt(AiConfigData record);

    List<AiConfigData> listPrompts();

    int insertAdvisor(AiConfigData record);

    List<AiConfigData> listAdvisors();

    int insertMcp(AiConfigData record);

    List<AiConfigData> listMcps();

    int insertConfig(AiConfigData record);

    List<AiConfigData> listConfigs();
}
