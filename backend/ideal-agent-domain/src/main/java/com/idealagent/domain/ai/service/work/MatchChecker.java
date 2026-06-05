package com.idealagent.domain.ai.service.work;

import com.idealagent.domain.ai.model.vo.AiFlowVO;
import com.idealagent.domain.ai.repository.IWorkAgentRepository;
import com.idealagent.domain.ai.service.armory.IChatClientArmory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class MatchChecker {
    private static final String MATCH_PROMPT = """
            你是任务匹配审核器。你只做一件事：判断“用户任务需求”是否与“智能体定位描述”匹配。
            输出规则必须严格遵守：
            1) 只能输出 YES 或 NO，禁止输出任何其他内容；
            2) 只有在任务与智能体定位完全不相关、或按定位完全不可能完成时，才输出 NO；
            3) 只要存在合理完成路径（即使需要拆解、联网、工具协作、迭代），都输出 YES。

            【智能体定位描述】
            %s
            【用户任务需求】
            %s
            现在只输出 YES 或 NO，严禁输出任何解释、代码和 markdown。
            """;

    private final IWorkAgentRepository repository;
    private final IChatClientArmory armory;
    private final WorkChatGateway chatGateway;

    public MatchChecker(IWorkAgentRepository repository, IChatClientArmory armory, WorkChatGateway chatGateway) {
        this.repository = repository;
        this.armory = armory;
        this.chatGateway = chatGateway;
    }

    public boolean isTaskMatched(String agentId, String agentDesc, String userMessage) {
        if (!StringUtils.hasText(agentDesc)) {
            return true;
        }
        try {
            AiFlowVO flow = repository.listFlowMap(agentId).values().stream().findFirst().orElse(null);
            if (flow == null || !StringUtils.hasText(flow.getClientId())) {
                return false;
            }
            ChatClient client = armory.resolve(flow.getClientId());
            String result = chatGateway.complete(client, MATCH_PROMPT.formatted(agentDesc, userMessage));
            return StringUtils.hasText(result) && result.trim().toUpperCase().contains("YES");
        } catch (Exception e) {
            return false;
        }
    }
}
