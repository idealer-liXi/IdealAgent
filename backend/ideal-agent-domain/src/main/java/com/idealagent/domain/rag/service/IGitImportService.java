package com.idealagent.domain.rag.service;

import com.idealagent.domain.rag.model.dto.RagUploadDTO;
import com.idealagent.domain.rag.model.entity.RagFile;

import java.util.List;

public interface IGitImportService {
    List<RagFile> importRepo(RagUploadDTO request);
}
