package com.idealagent.trigger.controller;

import com.idealagent.api.IAiApi;
import com.idealagent.domain.ai.model.dto.ChatRequestDTO;
import com.idealagent.domain.ai.model.dto.RagUploadDTO;
import com.idealagent.domain.ai.model.dto.WorkRequestDTO;
import com.idealagent.domain.ai.model.entity.ExecuteResponseEntity;
import com.idealagent.domain.ai.model.entity.RagFile;
import com.idealagent.domain.ai.model.vo.ChatClientOptionVO;
import com.idealagent.domain.ai.model.vo.ChatResponseVO;
import com.idealagent.domain.ai.model.vo.RagTagVO;
import com.idealagent.domain.ai.model.vo.WorkAgentOptionVO;
import com.idealagent.domain.ai.service.chat.ChatService;
import com.idealagent.domain.ai.service.rag.RagException;
import com.idealagent.domain.ai.service.rag.RagService;
import com.idealagent.domain.ai.service.work.WorkEventSink;
import com.idealagent.domain.ai.service.work.WorkService;
import com.idealagent.domain.session.model.vo.ChatMessageVO;
import com.idealagent.domain.session.model.vo.ChatSessionVO;
import com.idealagent.trigger.context.UserContext;
import com.idealagent.types.result.Result;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@RestController
@RequestMapping("/ai")
public class AiController implements IAiApi {
    private final ChatService chatService;
    private final RagService ragService;
    private final WorkService workService;
    private final Executor aiSseExecutor;

    public AiController(ChatService chatService, RagService ragService, WorkService workService, @Qualifier("aiSseExecutor") Executor aiSseExecutor) {
        this.chatService = chatService;
        this.ragService = ragService;
        this.workService = workService;
        this.aiSseExecutor = aiSseExecutor;
    }

    @PostMapping("/chat/complete")
    @Override
    public Result<ChatResponseVO> complete(@RequestBody ChatRequestDTO request) {
        return Result.success(chatService.send(UserContext.userId(), request));
    }

    @PostMapping("/chat/stream")
    @Override
    public SseEmitter stream(@RequestBody ChatRequestDTO request) {
        SseEmitter emitter = new SseEmitter(60_000L);
        Long userId = UserContext.userId();
        CompletableFuture.runAsync(() -> {
            try {
                ChatResponseVO response = chatService.stream(userId, request, delta -> {
                    if (!sendEvent(emitter, "delta", delta)) {
                        throw new StreamAbortedException();
                    }
                });
                sendEvent(emitter, "done", response);
            } catch (StreamAbortedException ignored) {
                // Client disconnected or timed out; do not write to the broken emitter again.
            } catch (Exception e) {
                sendEvent(emitter, "error", e.getMessage());
            } finally {
                complete(emitter);
            }
        }, aiSseExecutor);
        return emitter;
    }

    @PostMapping(value = "/work/execute", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Override
    public SseEmitter executeWork(@RequestBody WorkRequestDTO request) {
        SseEmitter emitter = new SseEmitter(60_000L);
        Long userId = UserContext.userId();
        CompletableFuture.runAsync(() -> {
            WorkEventSink sink = new WorkEventSink() {
                @Override
                public void message(ExecuteResponseEntity response) {
                    sendEvent(emitter, "message", response);
                }

                @Override
                public void complete(ExecuteResponseEntity response) {
                    sendEvent(emitter, "complete", response);
                }

                @Override
                public void error(String message) {
                    sendEvent(emitter, "error", message);
                }
            };
            try {
                workService.execute(userId, request, sink);
            } catch (Exception e) {
                sink.error(e.getMessage());
            } finally {
                complete(emitter);
            }
        }, aiSseExecutor);
        return emitter;
    }

    @GetMapping("/work/agents")
    @Override
    public Result<List<WorkAgentOptionVO>> workAgents() {
        return Result.success(workService.listAgents());
    }

    @GetMapping("/work/sessions")
    public Result<List<ChatSessionVO>> workSessions() {
        return Result.success(workService.listSessions(UserContext.userId()));
    }

    @GetMapping("/work/messages/{sessionId}")
    public Result<List<ChatMessageVO>> workMessages(@PathVariable String sessionId) {
        return Result.success(workService.listMessages(UserContext.userId(), sessionId));
    }

    @GetMapping("/chat/sessions")
    public Result<List<ChatSessionVO>> sessions() {
        return Result.success(chatService.listSessions(UserContext.userId()));
    }

    @GetMapping("/chat/messages/{sessionId}")
    public Result<List<ChatMessageVO>> messages(@PathVariable String sessionId) {
        return Result.success(chatService.listMessages(UserContext.userId(), sessionId));
    }

    @GetMapping("/chat/clients")
    public Result<List<ChatClientOptionVO>> clients() {
        return Result.success(chatService.listClients());
    }

    @GetMapping("/rag/tags")
    public Result<List<RagTagVO>> tags() {
        return Result.success(ragService.listTags(UserContext.userId()));
    }

    @PostMapping(value = "/rag/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Override
    public Result<Void> uploadFile(@RequestPart("ragTag") String ragTag, @RequestPart("fileList") List<MultipartFile> fileList) {
        ragService.uploadFiles(UserContext.userId(), ragTag, toRagFiles(fileList));
        return Result.success(null);
    }

    @PostMapping("/rag/git")
    @Override
    public Result<Void> uploadGitRepo(@RequestBody RagUploadDTO request) {
        ragService.uploadGitRepo(UserContext.userId(), request);
        return Result.success(null);
    }

    private List<RagFile> toRagFiles(List<MultipartFile> fileList) {
        List<RagFile> files = new ArrayList<>();
        for (MultipartFile file : fileList) {
            try {
                files.add(new RagFile(file.getOriginalFilename(), new String(file.getBytes(), StandardCharsets.UTF_8)));
            } catch (IOException e) {
                throw new RagException("文件读取失败", e);
            }
        }
        return files;
    }

    private boolean sendEvent(SseEmitter emitter, String name, Object data) {
        try {
            emitter.send(SseEmitter.event().name(name).data(data));
            return true;
        } catch (IOException e) {
            return false;
        } catch (IllegalStateException e) {
            return false;
        }
    }

    private void complete(SseEmitter emitter) {
        try {
            emitter.complete();
        } catch (IllegalStateException ignored) {
            // Emitter may already be completed after a disconnect or timeout.
        }
    }

    private static class StreamAbortedException extends RuntimeException {
    }
}
