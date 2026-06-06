package com.idealagent.domain.ai.service.rag;

import com.idealagent.domain.ai.model.dto.RagUploadDTO;
import com.idealagent.domain.ai.model.entity.RagChunk;
import com.idealagent.domain.ai.model.entity.RagFile;
import com.idealagent.domain.ai.model.vo.RagTagVO;
import com.idealagent.domain.ai.repository.IRagTagRepository;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionTextParser;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class RagService {
    private static final int RETRIEVE_LIMIT = 3;

    private final IRagVectorStore vectorStore;
    private final TokenTextSplitter tokenTextSplitter;
    private final IRagTagRepository ragTagRepository;
    private final IGitImportService gitImportService;

    public RagService(IRagVectorStore vectorStore, TokenTextSplitter tokenTextSplitter, IRagTagRepository ragTagRepository, IGitImportService gitImportService) {
        this.vectorStore = vectorStore;
        this.tokenTextSplitter = tokenTextSplitter;
        this.ragTagRepository = ragTagRepository;
        this.gitImportService = gitImportService;
    }

    public void uploadFiles(Long userId, String ragTag, List<RagFile> files) {
        validateUserAndTag(userId, ragTag);
        if (files == null || files.isEmpty()) {
            throw new RagException("文件不能为空");
        }
        List<Document> splitDocuments = new ArrayList<>();
        for (RagFile file : files) {
            Document document = new Document(file == null || file.content() == null ? "" : file.content());
            String source = file == null || !StringUtils.hasText(file.fileName()) ? "unknown" : file.fileName();
            document.getMetadata().put("source", source);
            for (Document splitDocument : tokenTextSplitter.apply(List.of(document))) {
                splitDocument.getMetadata().put("source", splitDocument.getMetadata().getOrDefault("source", source));
                splitDocument.getMetadata().put("knowledge", ragTag);
                splitDocument.getMetadata().put("userId", String.valueOf(userId));
                splitDocuments.add(splitDocument);
            }
        }
        if (splitDocuments.isEmpty()) {
            throw new RagException("文件内容为空");
        }
        vectorStore.add(splitDocuments);
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
        return ragTagRepository.listTags(userId).stream().map(RagTagVO::new).toList();
    }

    public List<RagChunk> retrieve(Long userId, String ragTag, String query) {
        return retrieve(userId, ragTag, query, RETRIEVE_LIMIT);
    }

    public List<RagChunk> retrieve(Long userId, String ragTag, String query, Integer topK) {
        return retrieve(userId, ragTag, query, topK, null);
    }

    public List<RagChunk> retrieve(Long userId, String ragTag, String query, Integer topK, String filterExpression) {
        if (!StringUtils.hasText(ragTag)) {
            return List.of();
        }
        validateUserAndTag(userId, ragTag);
        FilterExpressionBuilder builder = new FilterExpressionBuilder();
        Filter.Expression baseExpression = builder.and(
                builder.eq("knowledge", ragTag),
                builder.eq("userId", String.valueOf(userId))).build();
        SearchRequest request = SearchRequest.builder()
                .query(query)
                .topK(topK != null && topK > 0 ? topK : RETRIEVE_LIMIT)
                .filterExpression(combine(baseExpression, filterExpression))
                .build();
        return vectorStore.similaritySearch(request).stream()
                .map(document -> new RagChunk(document.getText(), source(document.getMetadata()), new float[0]))
                .toList();
    }

    private Filter.Expression combine(Filter.Expression baseExpression, String filterExpression) {
        if (!StringUtils.hasText(filterExpression)) {
            return baseExpression;
        }
        Filter.Expression advisorExpression = new FilterExpressionTextParser().parse(filterExpression);
        return new Filter.Expression(Filter.ExpressionType.AND, baseExpression, advisorExpression);
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

    private String source(Map<String, Object> metadata) {
        Object source = metadata.get("source");
        return source == null || !StringUtils.hasText(source.toString()) ? "unknown" : source.toString();
    }
}
