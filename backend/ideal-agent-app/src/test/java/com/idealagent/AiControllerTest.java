package com.idealagent;

import com.idealagent.config.WebMvcConfig;
import com.idealagent.domain.ai.model.dto.ChatRequestDTO;
import com.idealagent.domain.ai.model.dto.RagUploadDTO;
import com.idealagent.domain.ai.model.vo.ChatClientOptionVO;
import com.idealagent.domain.ai.model.vo.ChatResponseVO;
import com.idealagent.domain.ai.model.vo.RagTagVO;
import com.idealagent.domain.ai.service.chat.ChatService;
import com.idealagent.domain.ai.service.rag.RagService;
import com.idealagent.domain.session.model.vo.ChatMessageVO;
import com.idealagent.domain.session.model.vo.ChatSessionVO;
import com.idealagent.domain.user.model.vo.AuthUserVO;
import com.idealagent.domain.user.service.auth.ITokenParser;
import com.idealagent.trigger.controller.AiController;
import com.idealagent.trigger.interceptor.AuthInterceptor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.lang.reflect.Constructor;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AiController.class)
@Import({WebMvcConfig.class, AuthInterceptor.class, AiControllerTest.TestExecutorConfig.class})
class AiControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ChatService chatService;

    @MockitoBean
    private RagService ragService;

    @MockitoBean
    private ITokenParser tokenParser;

    @Test
    void controllerUsesExplicitExecutorForStreaming() throws Exception {
        Constructor<AiController> constructor = AiController.class.getConstructor(ChatService.class, RagService.class, Executor.class);

        assertThat(constructor).isNotNull();
    }

    @Test
    void completeReturnsAssistantMessageForCurrentUser() throws Exception {
        when(tokenParser.parseToken("token-1")).thenReturn(new AuthUserVO(7L, "alice", "user"));
        when(chatService.send(eq(7L), any(ChatRequestDTO.class)))
                .thenReturn(new ChatResponseVO("session_1", new ChatMessageVO("msg_2", "session_1", "assistant", "hi", null)));

        mockMvc.perform(post("/ai/chat/complete")
                        .header("Authorization", "Bearer token-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clientId\":\"client_default_chat\",\"content\":\"hello\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sessionId").value("session_1"))
                .andExpect(jsonPath("$.data.assistantMessage.content").value("hi"));
    }

    @Test
    void streamDelegatesToChatServiceForCurrentUser() throws Exception {
        when(tokenParser.parseToken("token-1")).thenReturn(new AuthUserVO(7L, "alice", "user"));
        when(chatService.stream(eq(7L), any(ChatRequestDTO.class), any())).thenAnswer(invocation -> {
            Consumer<String> onDelta = invocation.getArgument(2);
            onDelta.accept("hello-delta");
            return new ChatResponseVO("session_1", new ChatMessageVO("msg_2", "session_1", "assistant", "hi", null));
        });

        MvcResult result = mockMvc.perform(post("/ai/chat/stream")
                        .header("Authorization", "Bearer token-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clientId\":\"client_default_chat\",\"content\":\"hello\"}"))
                .andExpect(status().isOk())
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("event:delta")))
                .andExpect(content().string(containsString("hello-delta")))
                .andExpect(content().string(containsString("event:done")))
                .andExpect(content().string(containsString("session_1")));

        verify(chatService).stream(eq(7L), any(ChatRequestDTO.class), any());
    }

    @Test
    void sessionsReturnsCurrentUserSessions() throws Exception {
        when(tokenParser.parseToken("token-1")).thenReturn(new AuthUserVO(7L, "alice", "user"));
        when(chatService.listSessions(7L)).thenReturn(List.of(new ChatSessionVO("session_1", "hello", "client_default_chat", null, null)));

        mockMvc.perform(get("/ai/chat/sessions").header("Authorization", "Bearer token-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].sessionId").value("session_1"));
    }

    @Test
    void clientsReturnsClientOptions() throws Exception {
        when(tokenParser.parseToken("token-1")).thenReturn(new AuthUserVO(7L, "alice", "user"));
        when(chatService.listClients()).thenReturn(List.of(new ChatClientOptionVO(
                "client_default_chat", "Default Chat", "chat", "model_default_chat", "gpt-4o-mini", 1)));

        mockMvc.perform(get("/ai/chat/clients").header("Authorization", "Bearer token-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].clientId").value("client_default_chat"))
                .andExpect(jsonPath("$.data[0].modelName").value("gpt-4o-mini"));
    }

    @Test
    void tagsReturnsCurrentUserRagTags() throws Exception {
        when(tokenParser.parseToken("token-1")).thenReturn(new AuthUserVO(7L, "alice", "user"));
        when(ragService.listTags(7L)).thenReturn(List.of(new RagTagVO("spring-ai")));

        mockMvc.perform(get("/ai/rag/tags").header("Authorization", "Bearer token-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].ragTag").value("spring-ai"));
    }

    @Test
    void uploadFileDelegatesToRagService() throws Exception {
        when(tokenParser.parseToken("token-1")).thenReturn(new AuthUserVO(7L, "alice", "user"));
        MockMultipartFile file = new MockMultipartFile("fileList", "note.md", "text/markdown", "hello".getBytes());
        MockMultipartFile ragTag = new MockMultipartFile("ragTag", "", "text/plain", "spring-ai".getBytes());

        mockMvc.perform(multipart("/ai/rag/file")
                        .file(file)
                        .file(ragTag)
                        .header("Authorization", "Bearer token-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0000"));

        verify(ragService).uploadFiles(eq(7L), eq("spring-ai"), any(List.class));
    }

    @Test
    void uploadGitDelegatesToRagService() throws Exception {
        when(tokenParser.parseToken("token-1")).thenReturn(new AuthUserVO(7L, "alice", "user"));

        mockMvc.perform(post("/ai/rag/git")
                        .header("Authorization", "Bearer token-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ragTag\":\"repo\",\"repoUrl\":\"https://example.com/repo.git\"}"))
                .andExpect(status().isOk());

        verify(ragService).uploadGitRepo(eq(7L), any(RagUploadDTO.class));
    }

    @TestConfiguration
    static class TestExecutorConfig {
        @Bean("aiSseExecutor")
        Executor aiSseExecutor() {
            return Runnable::run;
        }
    }
}
