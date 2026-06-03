package com.idealagent.trigger.handler;

import com.idealagent.domain.user.service.auth.AuthException;
import com.idealagent.domain.ai.service.chat.ChatException;
import com.idealagent.domain.ai.service.config.AiConfigException;
import com.idealagent.domain.ai.service.mcp.McpException;
import com.idealagent.domain.ai.service.rag.RagException;
import com.idealagent.types.result.Result;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AuthException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Result<Void> handleAuthException(AuthException e) {
        return Result.failure(e.getMessage());
    }

    @ExceptionHandler(AiConfigException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleAiConfigException(AiConfigException e) {
        return Result.failure(e.getMessage());
    }

    @ExceptionHandler(ChatException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleChatException(ChatException e) {
        return Result.failure(e.getMessage());
    }

    @ExceptionHandler(RagException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleRagException(RagException e) {
        return Result.failure(e.getMessage());
    }

    @ExceptionHandler(McpException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleMcpException(McpException e) {
        return Result.failure(e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleException(Exception e) {
        return Result.failure("系统异常");
    }
}
