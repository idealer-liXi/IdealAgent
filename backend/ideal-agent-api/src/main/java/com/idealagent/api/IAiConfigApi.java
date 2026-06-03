package com.idealagent.api;

import com.idealagent.domain.ai.model.dto.AiConfigRecordDTO;
import com.idealagent.domain.ai.model.vo.AiConfigRecordVO;
import com.idealagent.types.result.Result;

import java.util.List;

public interface IAiConfigApi {
    Result<AiConfigRecordVO> create(String kind, AiConfigRecordDTO request);

    Result<List<AiConfigRecordVO>> list(String kind);

    Result<AiConfigRecordVO> update(String kind, String configId, AiConfigRecordDTO request);

    Result<Void> updateStatus(String kind, String configId, AiConfigRecordDTO request);

    Result<Void> delete(String kind, String configId);
}
