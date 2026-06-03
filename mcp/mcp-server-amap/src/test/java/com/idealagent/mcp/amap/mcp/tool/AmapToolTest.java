package com.idealagent.mcp.amap.mcp.tool;

import com.idealagent.mcp.amap.mcp.dto.CheckWeatherToolRequest;
import com.idealagent.mcp.amap.mcp.dto.CheckWeatherToolResponse;
import com.idealagent.mcp.amap.mcp.port.IAmapPort;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class AmapToolTest {

    @Test
    void checkWeatherDelegatesRequestToPort() throws IOException {
        AmapTool tool = new AmapTool();
        FakeAmapPort port = new FakeAmapPort();
        ReflectionTestUtils.setField(tool, "amapPort", port);

        CheckWeatherToolRequest request = new CheckWeatherToolRequest();
        request.setAddress("浙江省杭州市西湖区");

        CheckWeatherToolResponse response = tool.checkWeather(request);

        assertThat(port.receivedRequest).isSameAs(request);
        assertThat(response).isSameAs(port.response);
    }

    private static class FakeAmapPort implements IAmapPort {
        private final CheckWeatherToolResponse response = new CheckWeatherToolResponse();
        private CheckWeatherToolRequest receivedRequest;

        @Override
        public CheckWeatherToolResponse checkWeather(CheckWeatherToolRequest toolRequest) {
            receivedRequest = toolRequest;
            return response;
        }
    }
}
