package com.idealagent.infrastructure.persistent.dao;

import com.idealagent.infrastructure.persistent.po.AiAgent;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface IAiAgentDao {
    AiAgent queryByAgentId(@Param("agentId") String agentId);

    List<AiAgent> listEnabledWorkAgents();

    List<AiAgent> listAll();

    int insert(AiAgent agent);

    int update(AiAgent agent);

    int updateStatus(@Param("agentId") String agentId, @Param("status") Integer status);

    int delete(@Param("agentId") String agentId);
}
