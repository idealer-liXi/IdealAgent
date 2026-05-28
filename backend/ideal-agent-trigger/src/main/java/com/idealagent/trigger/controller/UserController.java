package com.idealagent.trigger.controller;

import com.idealagent.domain.auth.model.vo.UserProfileVO;
import com.idealagent.domain.auth.service.AuthException;
import com.idealagent.domain.auth.service.AuthService;
import com.idealagent.trigger.context.UserContext;
import com.idealagent.types.result.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user")
public class UserController {
    private final AuthService authService;

    public UserController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/profile")
    public Result<UserProfileVO> profile() {
        Long userId = UserContext.userId();
        if (userId == null) {
            throw new AuthException("未登录");
        }
        return Result.success(authService.profile(userId));
    }
}
