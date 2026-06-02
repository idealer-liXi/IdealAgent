package com.idealagent;

import com.idealagent.domain.auth.model.vo.AuthUserVO;
import com.idealagent.domain.auth.service.ITokenParser;
import com.idealagent.domain.chat.model.dto.ChatRequestDTO;
import com.idealagent.domain.chat.model.vo.ChatClientOptionVO;
import com.idealagent.domain.chat.model.vo.ChatMessageVO;
import com.idealagent.domain.chat.model.vo.ChatResponseVO;
import com.idealagent.domain.chat.model.vo.ChatSessionVO;
import com.idealagent.domain.chat.service.ChatService;
import com.idealagent.trigger.config.WebMvcConfig;
import com.idealagent.trigger.controller.ChatController;
import com.idealagent.trigger.interceptor.AuthInterceptor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ChatController.class)
@Import({WebMvcConfig.class, AuthInterceptor.class})
class ChatControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ChatService chatService;

    @MockitoBean
    private ITokenParser tokenParser;

    @Test
    void sendReturnsAssistantMessageForCurrentUser() throws Exception {
        when(tokenParser.parseToken("token-1")).thenReturn(new AuthUserVO(7L, "alice", "user"));
        when(chatService.send(eq(7L), any(ChatRequestDTO.class)))
                .thenReturn(new ChatResponseVO("session_1", new ChatMessageVO("msg_2", "session_1", "assistant", "hi", null)));

        mockMvc.perform(post("/chat/send")
                        .header("Authorization", "Bearer token-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clientId\":\"client_default_chat\",\"content\":\"hello\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sessionId").value("session_1"))
                .andExpect(jsonPath("$.data.assistantMessage.content").value("hi"));
    }

    @Test
    void listSessionsAndMessagesForCurrentUser() throws Exception {
        when(tokenParser.parseToken("token-1")).thenReturn(new AuthUserVO(7L, "alice", "user"));
        when(chatService.listSessions(7L)).thenReturn(List.of(new ChatSessionVO("session_1", "hello", "client_default_chat", null, null)));
        when(chatService.listMessages(7L, "session_1")).thenReturn(List.of(new ChatMessageVO("msg_1", "session_1", "user", "hello", null)));

        mockMvc.perform(get("/chat/sessions").header("Authorization", "Bearer token-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].sessionId").value("session_1"));

        mockMvc.perform(get("/chat/messages/session_1").header("Authorization", "Bearer token-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].content").value("hello"));
    }

    @Test
    void listChatClientsReturnsClientOptions() throws Exception {
        when(tokenParser.parseToken("token-1")).thenReturn(new AuthUserVO(7L, "alice", "user"));
        when(chatService.listClients()).thenReturn(List.of(new ChatClientOptionVO(
                "client_default_chat", "Default Chat", "chat", "model_default_chat", "gpt-4o-mini", 1)));

        mockMvc.perform(get("/chat/clients").header("Authorization", "Bearer token-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].clientId").value("client_default_chat"))
                .andExpect(jsonPath("$.data[0].modelName").value("gpt-4o-mini"));
    }
}
