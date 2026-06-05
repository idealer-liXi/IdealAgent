package com.idealagent.domain.ai.service.work;

import com.idealagent.domain.ai.model.entity.ExecuteRequestEntity;

public interface IExecuteStrategy {
    void execute(ExecuteRequestEntity request, WorkEventSink sink);

    String getType();
}
