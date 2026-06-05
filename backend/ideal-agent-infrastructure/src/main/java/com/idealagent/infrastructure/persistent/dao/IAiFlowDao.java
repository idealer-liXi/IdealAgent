package com.idealagent.infrastructure.persistent.dao;

import com.idealagent.infrastructure.persistent.po.AiFlow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface IAiFlowDao {
    List<AiFlow> listEnabledByAgentId(@Param("agentId") String agentId);

    List<AiFlow> listByAgentId(@Param("agentId") String agentId);

    AiFlow queryByFlowId(@Param("flowId") String flowId);

    int insert(AiFlow flow);

    int update(AiFlow flow);

    int updateStatus(@Param("flowId") String flowId, @Param("status") Integer status);

    int delete(@Param("flowId") String flowId);
}
