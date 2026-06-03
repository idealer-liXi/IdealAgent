package com.idealagent.mcp.bocha.mcp.port;

import com.idealagent.mcp.bocha.mcp.dto.BochaSearchToolRequest;
import com.idealagent.mcp.bocha.mcp.dto.BochaSearchToolResponse;

import java.io.IOException;

public interface IBochaPort {

    BochaSearchToolResponse webSearch(BochaSearchToolRequest toolRequest) throws IOException;
}
