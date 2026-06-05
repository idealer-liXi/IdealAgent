package com.idealagent.domain.ai.service.work;

public class WorkException extends RuntimeException {
    public WorkException(String message) {
        super(message);
    }

    public WorkException(String message, Throwable cause) {
        super(message, cause);
    }
}
