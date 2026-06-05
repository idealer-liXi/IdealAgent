package com.idealagent.mcp.wecom.mcp.port;

import com.idealagent.mcp.wecom.mcp.dto.SendMessageToolResponse;
import com.idealagent.mcp.wecom.mcp.dto.SendTextCardToolRequest;
import com.idealagent.mcp.wecom.mcp.dto.SendTextToolRequest;

import java.io.IOException;

public interface IWeComPort {

    SendMessageToolResponse sendTextCard(SendTextCardToolRequest toolRequest) throws IOException;

    SendMessageToolResponse sendText(SendTextToolRequest toolRequest) throws IOException;
}
