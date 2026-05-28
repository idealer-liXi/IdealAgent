package com.idealagent.trigger.controller;

import com.idealagent.domain.auth.model.dto.LoginDTO;
import com.idealagent.domain.auth.model.dto.RegisterDTO;
import com.idealagent.domain.auth.model.vo.AuthTokenVO;
import com.idealagent.domain.auth.service.AuthService;
import com.idealagent.types.result.Result;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public Result<AuthTokenVO> register(@RequestBody RegisterDTO request) {
        return Result.success(authService.register(request));
    }

    @PostMapping("/login")
    public Result<AuthTokenVO> login(@RequestBody LoginDTO request) {
        return Result.success(authService.login(request));
    }
}
