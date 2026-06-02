package com.idealagent.trigger.controller;

import com.idealagent.api.IRagApi;
import com.idealagent.domain.ai.model.dto.RagUploadDTO;
import com.idealagent.domain.ai.model.entity.RagFile;
import com.idealagent.domain.ai.model.vo.RagTagVO;
import com.idealagent.domain.ai.service.rag.RagException;
import com.idealagent.domain.ai.service.rag.RagService;
import com.idealagent.trigger.context.UserContext;
import com.idealagent.types.result.Result;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/rag")
public class RagController implements IRagApi {
    private final RagService ragService;

    public RagController(RagService ragService) {
        this.ragService = ragService;
    }

    @GetMapping("/tags")
    @Override
    public Result<List<RagTagVO>> tags() {
        return Result.success(ragService.listTags(UserContext.userId()));
    }

    @PostMapping(value = "/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Override
    public Result<Void> uploadFile(@RequestPart("ragTag") String ragTag, @RequestPart("fileList") List<MultipartFile> fileList) {
        ragService.uploadFiles(UserContext.userId(), ragTag, toRagFiles(fileList));
        return Result.success(null);
    }

    @PostMapping("/git")
    @Override
    public Result<Void> uploadGit(@RequestBody RagUploadDTO request) {
        ragService.uploadGitRepo(UserContext.userId(), request);
        return Result.success(null);
    }

    private List<RagFile> toRagFiles(List<MultipartFile> fileList) {
        List<RagFile> files = new ArrayList<>();
        for (MultipartFile file : fileList) {
            try {
                files.add(new RagFile(file.getOriginalFilename(), new String(file.getBytes(), StandardCharsets.UTF_8)));
            } catch (IOException e) {
                throw new RagException("文件读取失败", e);
            }
        }
        return files;
    }
}
