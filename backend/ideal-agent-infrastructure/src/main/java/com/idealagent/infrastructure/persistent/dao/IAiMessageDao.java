package com.idealagent.infrastructure.persistent.dao;

import com.idealagent.infrastructure.persistent.po.AiMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface IAiMessageDao {
    int insert(AiMessage message);

    List<AiMessage> listBySessionId(@Param("sessionId") String sessionId);
}
