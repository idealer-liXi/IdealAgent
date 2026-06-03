package com.idealagent.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan(basePackages = "com.idealagent.infrastructure.persistent.dao", sqlSessionTemplateRef = "mysqlTemplate")
public class MyBatisConfig {
}
