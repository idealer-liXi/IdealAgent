package com.idealagent.mcp.amap;

import com.idealagent.mcp.amap.mcp.tool.AmapTool;
import com.idealagent.mcp.amap.sse.http.IAmapHttp;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

@SpringBootApplication
public class McpServerAmapApplication {

    private static final String AMAP_BASE_URL = "https://restapi.amap.com/v3/";

    public static void main(String[] args) {
        SpringApplication.run(McpServerAmapApplication.class, args);
    }

    @Bean
    public IAmapHttp createHttp() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(AMAP_BASE_URL)
                .addConverterFactory(JacksonConverterFactory.create())
                .build();
        return retrofit.create(IAmapHttp.class);
    }

    @Bean
    public ToolCallbackProvider createTool(AmapTool amapTool) {
        return MethodToolCallbackProvider.builder().toolObjects(amapTool).build();
    }
}
