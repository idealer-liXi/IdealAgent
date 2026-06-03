package com.idealagent.config;

import com.idealagent.infrastructure.util.JwtUtil;
import com.idealagent.properties.JwtProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class JwtConfig {
    @Bean(name = {"jwtUtil", "tokenService", "tokenParser"})
    public JwtUtil jwtUtil(JwtProperties properties) {
        return new JwtUtil(properties.getIssuer(), properties.getSecret(), properties.getExpireHours());
    }
}
