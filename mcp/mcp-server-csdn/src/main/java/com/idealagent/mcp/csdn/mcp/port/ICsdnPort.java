package com.idealagent.mcp.csdn.mcp.port;

import com.idealagent.mcp.csdn.mcp.dto.SaveArticleToolRequest;
import com.idealagent.mcp.csdn.mcp.dto.SaveArticleToolResponse;

import java.io.IOException;

public interface ICsdnPort {

    SaveArticleToolResponse saveArticle(SaveArticleToolRequest request) throws IOException;
}
