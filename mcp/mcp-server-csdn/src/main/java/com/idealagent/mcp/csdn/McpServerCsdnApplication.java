package com.idealagent.mcp.csdn;

import com.idealagent.mcp.csdn.mcp.tool.CsdnTool;
import com.idealagent.mcp.csdn.sse.http.ICsdnHttp;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

@SpringBootApplication
public class McpServerCsdnApplication {

    private static final String CSDN_BASE_URL = "https://bizapi.csdn.net/";

    public static void main(String[] args) {
        SpringApplication.run(McpServerCsdnApplication.class, args);
    }

    @Bean
    public ICsdnHttp createHttp() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(CSDN_BASE_URL)
                .addConverterFactory(JacksonConverterFactory.create())
                .build();
        return retrofit.create(ICsdnHttp.class);
    }

    @Bean
    public ToolCallbackProvider createTool(CsdnTool csdnTool) {
        return MethodToolCallbackProvider.builder().toolObjects(csdnTool).build();
    }
}
