package com.idealagent.api;

import com.idealagent.domain.ai.model.dto.ChatRequestDTO;
import com.idealagent.domain.ai.model.dto.RagUploadDTO;
import com.idealagent.domain.ai.model.dto.WorkRequestDTO;
import com.idealagent.domain.ai.model.vo.ChatResponseVO;
import com.idealagent.domain.ai.model.vo.WorkAgentOptionVO;
import com.idealagent.types.result.Result;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

public interface IAiApi {
    Result<ChatResponseVO> complete(ChatRequestDTO request);

    SseEmitter stream(ChatRequestDTO request);

    SseEmitter executeWork(WorkRequestDTO request);

    Result<List<WorkAgentOptionVO>> workAgents();

    Result<Void> uploadFile(String ragTag, List<MultipartFile> fileList);

    Result<Void> uploadGitRepo(RagUploadDTO request);
}
