package com.idealagent.domain.ai.service.work;

import com.idealagent.domain.ai.model.entity.ExecuteResponseEntity;

public interface WorkEventSink {
    void message(ExecuteResponseEntity response);

    void complete(ExecuteResponseEntity response);

    void error(String message);
}
