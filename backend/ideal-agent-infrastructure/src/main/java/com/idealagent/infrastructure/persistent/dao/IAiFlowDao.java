package com.idealagent.infrastructure.persistent.dao;

import com.idealagent.infrastructure.persistent.po.AiFlow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface IAiFlowDao {
    List<AiFlow> listEnabledByAgentId(@Param("agentId") String agentId);

    List<AiFlow> listByAgentId(@Param("agentId") String agentId);

    AiFlow queryByAgentIdAndClientId(@Param("agentId") String agentId, @Param("clientId") String clientId);

    int insert(AiFlow flow);

    int update(@Param("originAgentId") String originAgentId, @Param("originClientId") String originClientId, @Param("flow") AiFlow flow);

    int deleteByAgentIdAndClientId(@Param("agentId") String agentId, @Param("clientId") String clientId);

    int deleteByAgentId(@Param("agentId") String agentId);
}
