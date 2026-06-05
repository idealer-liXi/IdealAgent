package com.idealagent.infrastructure.persistent.dao;

import com.idealagent.infrastructure.persistent.po.AiSession;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface IAiSessionDao {
    int insert(AiSession session);

    AiSession queryBySessionIdAndUserId(@Param("sessionId") String sessionId, @Param("userId") Long userId);

    AiSession queryBySessionIdAndUserIdAndType(@Param("sessionId") String sessionId, @Param("userId") Long userId, @Param("type") String type);

    List<AiSession> listByUserId(@Param("userId") Long userId);

    List<AiSession> listByUserIdAndType(@Param("userId") Long userId, @Param("type") String type);
}
