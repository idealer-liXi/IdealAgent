package com.idealagent.mcp.wecom.credential;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

@Slf4j
@Component
public class McpWebFilter implements WebFilter {

    @Resource
    private ObjectMapper objectMapper;

    @Value("${ideal-agent.mcp.header.secret:X-IdealAgent-Mcp-Secret}")
    private String mcpSecretHeader;

    @Value("${ideal-agent.mcp.header.user-id:X-IdealAgent-Mcp-UserId}")
    private String mcpUserIdHeader;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String encodedSecret = exchange.getRequest().getHeaders().getFirst(mcpSecretHeader);
        String userId = exchange.getRequest().getHeaders().getFirst(mcpUserIdHeader);

        WeComCredential credential = parseCredential(encodedSecret, userId);
        return chain.filter(exchange)
                .contextWrite(context -> context.put(McpHeaderContext.CONTEXT_KEY, credential));
    }

    private WeComCredential parseCredential(String encodedSecret, String userId) {
        try {
            if (!StringUtils.hasText(userId) || !StringUtils.hasText(encodedSecret)) {
                return new WeComCredential();
            }

            byte[] decodedBytes = Base64.getDecoder().decode(encodedSecret);
            String secretJson = new String(decodedBytes, StandardCharsets.UTF_8);
            if (!StringUtils.hasText(secretJson)) {
                return new WeComCredential();
            }

            Map<String, String> secretMap = objectMapper.readValue(secretJson, new TypeReference<>() {});
            WeComCredential weComCredential = WeComCredential.builder()
                    .corpId(secretMap.get("corpId"))
                    .corpSecret(secretMap.get("corpSecret"))
                    .agentId(parseAgentId(secretMap.get("agentId")))
                    .userId(userId)
                    .build();

            log.info("【Header 解析】wecom credential 解析完成, userId={}", userId);
            return weComCredential;
        } catch (Exception e) {
            log.error("【Header 解析】wecom credential 解析失败, userId={}", userId, e);
            return new WeComCredential();
        }
    }

    private Integer parseAgentId(String agentIdRaw) {
        if (!StringUtils.hasText(agentIdRaw)) {
            return null;
        }
        try {
            return Integer.parseInt(agentIdRaw);
        } catch (Exception e) {
            return null;
        }
    }
}
