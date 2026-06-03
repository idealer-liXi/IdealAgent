package com.idealagent.mcp.amap.mcp.port;

import com.idealagent.mcp.amap.mcp.dto.CheckWeatherToolRequest;
import com.idealagent.mcp.amap.mcp.dto.CheckWeatherToolResponse;

import java.io.IOException;

public interface IAmapPort {

    CheckWeatherToolResponse checkWeather(CheckWeatherToolRequest toolRequest) throws IOException;
}
