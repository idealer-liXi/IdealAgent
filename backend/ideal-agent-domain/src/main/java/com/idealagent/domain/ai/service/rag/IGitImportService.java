package com.idealagent.domain.ai.service.rag;

import com.idealagent.domain.ai.model.dto.RagUploadDTO;
import com.idealagent.domain.ai.model.entity.RagFile;

import java.util.List;

public interface IGitImportService {
    List<RagFile> importRepo(RagUploadDTO request);
}
