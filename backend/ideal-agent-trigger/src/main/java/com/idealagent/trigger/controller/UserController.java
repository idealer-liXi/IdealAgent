package com.idealagent.trigger.controller;

import com.idealagent.api.IUserApi;
import com.idealagent.domain.user.model.vo.UserProfileVO;
import com.idealagent.domain.user.service.auth.AuthException;
import com.idealagent.domain.user.service.auth.AuthService;
import com.idealagent.trigger.context.UserContext;
import com.idealagent.types.result.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user")
public class UserController implements IUserApi {
    private final AuthService authService;

    public UserController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/profile")
    @Override
    public Result<UserProfileVO> profile() {
        Long userId = UserContext.userId();
        if (userId == null) {
            throw new AuthException("未登录");
        }
        return Result.success(authService.profile(userId));
    }
}
