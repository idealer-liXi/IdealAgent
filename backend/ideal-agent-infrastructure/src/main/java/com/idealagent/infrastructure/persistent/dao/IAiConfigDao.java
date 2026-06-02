package com.idealagent.infrastructure.persistent.dao;

import com.idealagent.infrastructure.persistent.po.AiConfigData;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface IAiConfigDao {
    int insertApi(AiConfigData record);

    List<AiConfigData> listApis();

    int updateApi(AiConfigData record);

    int updateApiStatus(@Param("configId") String configId, @Param("status") Integer status);

    int deleteApi(String configId);

    int insertModel(AiConfigData record);

    List<AiConfigData> listModels();

    int updateModel(AiConfigData record);

    int updateModelStatus(@Param("configId") String configId, @Param("status") Integer status);

    int deleteModel(String configId);

    int insertClient(AiConfigData record);

    List<AiConfigData> listClients();

    int updateClient(AiConfigData record);

    int updateClientStatus(@Param("configId") String configId, @Param("status") Integer status);

    int deleteClient(String configId);

    int insertPrompt(AiConfigData record);

    List<AiConfigData> listPrompts();

    int updatePrompt(AiConfigData record);

    int updatePromptStatus(@Param("configId") String configId, @Param("status") Integer status);

    int deletePrompt(String configId);

    int insertAdvisor(AiConfigData record);

    List<AiConfigData> listAdvisors();

    int updateAdvisor(AiConfigData record);

    int updateAdvisorStatus(@Param("configId") String configId, @Param("status") Integer status);

    int deleteAdvisor(String configId);

    int insertMcp(AiConfigData record);

    List<AiConfigData> listMcps();

    int updateMcp(AiConfigData record);

    int updateMcpStatus(@Param("configId") String configId, @Param("status") Integer status);

    int deleteMcp(String configId);

    int insertConfig(AiConfigData record);

    List<AiConfigData> listConfigs();

    int updateConfig(AiConfigData record);

    int updateConfigStatus(@Param("configId") String configId, @Param("status") Integer status);

    int deleteConfig(String configId);
}
