package com.idealagent.mcp.email.mcp.port;

import com.idealagent.mcp.email.mcp.dto.SendEmailToolRequest;
import com.idealagent.mcp.email.mcp.dto.SendEmailToolResponse;

import java.io.IOException;

public interface IEmailPort {

    SendEmailToolResponse sendEmail(SendEmailToolRequest toolRequest) throws IOException;
}
