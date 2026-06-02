package com.idealagent.trigger.controller;

import com.idealagent.api.IChatApi;
import com.idealagent.domain.ai.model.dto.ChatRequestDTO;
import com.idealagent.domain.ai.model.vo.ChatClientOptionVO;
import com.idealagent.domain.session.model.vo.ChatMessageVO;
import com.idealagent.domain.ai.model.vo.ChatResponseVO;
import com.idealagent.domain.session.model.vo.ChatSessionVO;
import com.idealagent.domain.ai.service.chat.ChatService;
import com.idealagent.trigger.context.UserContext;
import com.idealagent.types.result.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/chat")
public class ChatController implements IChatApi {
    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping("/send")
    @Override
    public Result<ChatResponseVO> send(@RequestBody ChatRequestDTO request) {
        return Result.success(chatService.send(UserContext.userId(), request));
    }

    @GetMapping("/sessions")
    @Override
    public Result<List<ChatSessionVO>> sessions() {
        return Result.success(chatService.listSessions(UserContext.userId()));
    }

    @GetMapping("/messages/{sessionId}")
    @Override
    public Result<List<ChatMessageVO>> messages(@PathVariable String sessionId) {
        return Result.success(chatService.listMessages(UserContext.userId(), sessionId));
    }

    @GetMapping("/clients")
    @Override
    public Result<List<ChatClientOptionVO>> clients() {
        return Result.success(chatService.listClients());
    }

    @PostMapping("/stream")
    @Override
    public SseEmitter stream(@RequestBody ChatRequestDTO request) {
        SseEmitter emitter = new SseEmitter(60_000L);
        Long userId = UserContext.userId();
        CompletableFuture.runAsync(() -> {
            try {
                ChatResponseVO response = chatService.stream(userId, request, delta -> sendEvent(emitter, "delta", delta));
                sendEvent(emitter, "done", response);
                emitter.complete();
            } catch (Exception e) {
                sendEvent(emitter, "error", e.getMessage());
                emitter.complete();
            }
        });
        return emitter;
    }

    private void sendEvent(SseEmitter emitter, String name, Object data) {
        try {
            emitter.send(SseEmitter.event().name(name).data(data));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
