package com.idealagent.mcp.wecom;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.idealagent.mcp.wecom.mcp.tool.WeComTool;
import com.idealagent.mcp.wecom.sse.http.IWeComHttp;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

import java.time.Duration;

@SpringBootApplication
@EnableCaching
public class McpServerWeComApplication {

    private static final String WECOM_BASE_URL = "https://qyapi.weixin.qq.com/";

    public static void main(String[] args) {
        SpringApplication.run(McpServerWeComApplication.class, args);
    }

    @Bean
    public IWeComHttp createHttp() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(WECOM_BASE_URL)
                .addConverterFactory(JacksonConverterFactory.create())
                .build();
        return retrofit.create(IWeComHttp.class);
    }

    @Bean
    public ToolCallbackProvider createTool(WeComTool weComTool) {
        return MethodToolCallbackProvider.builder().toolObjects(weComTool).build();
    }

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager("WeComAccessToken");
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(100)
                .expireAfterWrite(Duration.ofHours(3)));
        return cacheManager;
    }
}
