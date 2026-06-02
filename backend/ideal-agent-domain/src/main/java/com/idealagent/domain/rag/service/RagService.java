package com.idealagent.domain.rag.service;

import com.idealagent.domain.rag.model.dto.RagUploadDTO;
import com.idealagent.domain.rag.model.entity.RagChunk;
import com.idealagent.domain.rag.model.entity.RagFile;
import com.idealagent.domain.rag.model.vo.RagTagVO;
import com.idealagent.domain.rag.repository.IRagRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Service
public class RagService {
    private static final int RETRIEVE_LIMIT = 3;

    private final IRagRepository ragRepository;
    private final ITextSplitter textSplitter;
    private final IEmbeddingService embeddingService;
    private final IGitImportService gitImportService;

    public RagService(IRagRepository ragRepository, ITextSplitter textSplitter, IEmbeddingService embeddingService, IGitImportService gitImportService) {
        this.ragRepository = ragRepository;
        this.textSplitter = textSplitter;
        this.embeddingService = embeddingService;
        this.gitImportService = gitImportService;
    }

    public void uploadFiles(Long userId, String ragTag, List<RagFile> files) {
        validateUserAndTag(userId, ragTag);
        if (files == null || files.isEmpty()) {
            throw new RagException("文件不能为空");
        }
        List<RagChunk> chunks = new ArrayList<>();
        for (RagFile file : files) {
            for (String content : textSplitter.split(file.content())) {
                chunks.add(new RagChunk(content, file.fileName(), embeddingService.embed(content)));
            }
        }
        if (chunks.isEmpty()) {
            throw new RagException("文件内容为空");
        }
        ragRepository.saveChunks(userId, ragTag, chunks);
    }

    public void uploadGitRepo(Long userId, RagUploadDTO request) {
        if (gitImportService == null) {
            throw new RagException("Git 导入服务不可用");
        }
        String ragTag = StringUtils.hasText(request.ragTag()) ? request.ragTag() : tagFromRepoUrl(request.repoUrl());
        uploadFiles(userId, ragTag, gitImportService.importRepo(request));
    }

    public List<RagTagVO> listTags(Long userId) {
        if (userId == null) {
            throw new RagException("用户未登录");
        }
        return ragRepository.listTags(userId).stream().map(RagTagVO::new).toList();
    }

    public List<RagChunk> retrieve(Long userId, String ragTag, String query) {
        if (!StringUtils.hasText(ragTag)) {
            return List.of();
        }
        validateUserAndTag(userId, ragTag);
        return ragRepository.search(userId, ragTag, embeddingService.embed(query), RETRIEVE_LIMIT);
    }

    private void validateUserAndTag(Long userId, String ragTag) {
        if (userId == null) {
            throw new RagException("用户未登录");
        }
        if (!StringUtils.hasText(ragTag)) {
            throw new RagException("知识库标签不能为空");
        }
    }

    private String tagFromRepoUrl(String repoUrl) {
        if (!StringUtils.hasText(repoUrl)) {
            throw new RagException("Git 地址不能为空");
        }
        String normalized = repoUrl.endsWith(".git") ? repoUrl.substring(0, repoUrl.length() - 4) : repoUrl;
        int slash = normalized.lastIndexOf('/');
        return slash >= 0 ? normalized.substring(slash + 1) : normalized;
    }
}
