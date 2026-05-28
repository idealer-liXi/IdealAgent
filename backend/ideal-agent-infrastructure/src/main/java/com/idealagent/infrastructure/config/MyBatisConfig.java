package com.idealagent.infrastructure.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan("com.idealagent.infrastructure.persistent.dao")
public class MyBatisConfig {
}
