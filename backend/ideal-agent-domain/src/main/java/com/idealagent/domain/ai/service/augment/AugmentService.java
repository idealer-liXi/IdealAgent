package com.idealagent.domain.ai.service.augment;

import com.idealagent.domain.ai.model.entity.RagChunk;
import com.idealagent.domain.ai.service.rag.RagService;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AugmentService implements IAugmentService {
    static final String RAG_SYSTEM_PROMPT = """
            你是一个检索增强问答助手（RAG），你会收到一段参考资料（DOCUMENTS）。

            请严格遵守以下规则：
            - 事实依据：所有可核验的事实必须来自 DOCUMENTS；不要引入 DOCUMENTS 之外的具体事实、数字、名称、结论；
            - 输出约束：用简体中文回答，优先条目化，简洁直接；
            - 空处理：如果 DOCUMENTS 内容为空，就直接当作什么都没有提供，直接回答即可。

            DOCUMENTS:
            {documents}
            """;

    private final RagService ragService;

    public AugmentService(RagService ragService) {
        this.ragService = ragService;
    }

    @Override
    public List<Message> augmentRagMessage(Long userId, String userMessage, String ragTag) {
        return augmentRagMessage(userId, userMessage, ragTag, null);
    }

    @Override
    public List<Message> augmentRagMessage(Long userId, String userMessage, String ragTag, Integer topK) {
        return augmentRagMessage(userId, userMessage, ragTag, topK, null);
    }

    @Override
    public List<Message> augmentRagMessage(Long userId, String userMessage, String ragTag, Integer topK, String filterExpression) {
        if (!StringUtils.hasText(ragTag)) {
            return List.of(new UserMessage(userMessage));
        }
        List<RagChunk> chunks;
        if (StringUtils.hasText(filterExpression)) {
            chunks = ragService.retrieve(userId, ragTag, userMessage, topK, filterExpression);
        } else if (topK != null) {
            chunks = ragService.retrieve(userId, ragTag, userMessage, topK);
        } else {
            chunks = ragService.retrieve(userId, ragTag, userMessage);
        }
        String documents = chunks.stream()
                .map(RagChunk::content)
                .collect(Collectors.joining("\n"));
        return List.of(
                new SystemPromptTemplate(RAG_SYSTEM_PROMPT).createMessage(Map.of("documents", documents)),
                new UserMessage(userMessage)
        );
    }
}
