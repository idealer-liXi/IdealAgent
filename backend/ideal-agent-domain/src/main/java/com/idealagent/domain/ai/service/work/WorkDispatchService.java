package com.idealagent.domain.ai.service.work;

import com.idealagent.domain.ai.model.entity.ExecuteRequestEntity;
import com.idealagent.domain.ai.repository.IWorkAgentRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class WorkDispatchService {
    private final IWorkAgentRepository repository;
    private final Map<String, IExecuteStrategy> strategies;

    public WorkDispatchService(IWorkAgentRepository repository, List<IExecuteStrategy> strategies) {
        this.repository = repository;
        this.strategies = strategies.stream().collect(Collectors.toMap(IExecuteStrategy::getType, Function.identity()));
    }

    public void dispatch(ExecuteRequestEntity request, WorkEventSink sink) {
        String type = repository.findExecuteType(request.getAgentId());
        if (!StringUtils.hasText(type) || !strategies.containsKey(type)) {
            throw new WorkException("Work 执行策略不支持");
        }
        strategies.get(type).execute(request, sink);
    }
}
