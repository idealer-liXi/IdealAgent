package com.idealagent;

import com.idealagent.domain.auth.model.dto.LoginDTO;
import com.idealagent.domain.auth.model.dto.RegisterDTO;
import com.idealagent.domain.auth.model.vo.AuthTokenVO;
import com.idealagent.domain.auth.model.vo.AuthUserVO;
import com.idealagent.domain.auth.model.vo.UserProfileVO;
import com.idealagent.domain.auth.service.AuthService;
import com.idealagent.domain.auth.service.ITokenParser;
import com.idealagent.trigger.config.WebMvcConfig;
import com.idealagent.trigger.controller.AuthController;
import com.idealagent.trigger.controller.UserController;
import com.idealagent.trigger.interceptor.AuthInterceptor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {AuthController.class, UserController.class})
@Import({WebMvcConfig.class, AuthInterceptor.class})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @MockBean
    private ITokenParser tokenParser;

    @Test
    void registerReturnsTokenAndProfile() throws Exception {
        when(authService.register(any(RegisterDTO.class)))
                .thenReturn(new AuthTokenVO("token-1", new UserProfileVO(1L, "alice", "user", null)));

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userName\":\"alice\",\"password\":\"secret123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0000"))
                .andExpect(jsonPath("$.data.token").value("token-1"))
                .andExpect(jsonPath("$.data.profile.userName").value("alice"));
    }

    @Test
    void loginReturnsTokenAndProfile() throws Exception {
        when(authService.login(any(LoginDTO.class)))
                .thenReturn(new AuthTokenVO("token-1", new UserProfileVO(1L, "alice", "user", null)));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userName\":\"alice\",\"password\":\"secret123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").value("token-1"));
    }

    @Test
    void profileRequiresAuthorizationHeader() throws Exception {
        mockMvc.perform(get("/user/profile"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("0001"));
    }

    @Test
    void profileReturnsCurrentUser() throws Exception {
        when(tokenParser.parseToken("token-1"))
                .thenReturn(new AuthUserVO(1L, "alice", "user"));
        when(authService.profile(1L))
                .thenReturn(new UserProfileVO(1L, "alice", "user", null));

        mockMvc.perform(get("/user/profile")
                        .header("Authorization", "Bearer token-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userName").value("alice"));
    }
}
