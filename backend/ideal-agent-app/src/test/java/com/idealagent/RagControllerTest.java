package com.idealagent;

import com.idealagent.domain.auth.model.vo.AuthUserVO;
import com.idealagent.domain.auth.service.ITokenParser;
import com.idealagent.domain.rag.model.dto.RagUploadDTO;
import com.idealagent.domain.rag.model.entity.RagFile;
import com.idealagent.domain.rag.model.vo.RagTagVO;
import com.idealagent.domain.rag.service.RagService;
import com.idealagent.trigger.config.WebMvcConfig;
import com.idealagent.trigger.controller.RagController;
import com.idealagent.trigger.interceptor.AuthInterceptor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = RagController.class)
@Import({WebMvcConfig.class, AuthInterceptor.class})
class RagControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RagService ragService;

    @MockitoBean
    private ITokenParser tokenParser;

    @Test
    void tagsReturnsCurrentUserRagTags() throws Exception {
        when(tokenParser.parseToken("token-1")).thenReturn(new AuthUserVO(7L, "alice", "user"));
        when(ragService.listTags(7L)).thenReturn(List.of(new RagTagVO("spring-ai")));

        mockMvc.perform(get("/rag/tags").header("Authorization", "Bearer token-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].ragTag").value("spring-ai"));
    }

    @Test
    void uploadFileDelegatesToRagService() throws Exception {
        when(tokenParser.parseToken("token-1")).thenReturn(new AuthUserVO(7L, "alice", "user"));
        MockMultipartFile file = new MockMultipartFile("fileList", "note.md", "text/markdown", "hello".getBytes());
        MockMultipartFile ragTag = new MockMultipartFile("ragTag", "", "text/plain", "spring-ai".getBytes());

        mockMvc.perform(multipart("/rag/file")
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

        mockMvc.perform(post("/rag/git")
                        .header("Authorization", "Bearer token-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ragTag\":\"repo\",\"repoUrl\":\"https://example.com/repo.git\"}"))
                .andExpect(status().isOk());

        verify(ragService).uploadGitRepo(eq(7L), any(RagUploadDTO.class));
    }
}
