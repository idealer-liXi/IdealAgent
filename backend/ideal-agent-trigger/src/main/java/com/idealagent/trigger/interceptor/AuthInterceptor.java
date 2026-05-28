package com.idealagent.trigger.interceptor;

import com.idealagent.domain.auth.model.vo.AuthUserVO;
import com.idealagent.domain.auth.service.ITokenParser;
import com.idealagent.trigger.context.UserContext;
import com.idealagent.types.result.Result;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;

@Component
public class AuthInterceptor implements HandlerInterceptor {
    private final ITokenParser tokenParser;
    private final ObjectMapper objectMapper;

    public AuthInterceptor(ITokenParser tokenParser, ObjectMapper objectMapper) {
        this.tokenParser = tokenParser;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String authorization = request.getHeader("Authorization");
        if (!StringUtils.hasText(authorization) || !authorization.startsWith("Bearer ")) {
            writeUnauthorized(response, "未登录");
            return false;
        }

        String token = authorization.substring("Bearer ".length());
        AuthUserVO user = tokenParser.parseToken(token);
        UserContext.set(user);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }

    private void writeUnauthorized(HttpServletResponse response, String message) throws Exception {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(Result.failure(message)));
    }
}
