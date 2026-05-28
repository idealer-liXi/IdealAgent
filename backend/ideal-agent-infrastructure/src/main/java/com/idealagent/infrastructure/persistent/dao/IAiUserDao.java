package com.idealagent.infrastructure.persistent.dao;

import com.idealagent.infrastructure.persistent.po.AiUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface IAiUserDao {
    AiUser queryByUserName(@Param("userName") String userName);

    AiUser queryById(@Param("id") Long id);

    int insert(AiUser aiUser);
}
