package com.idealagent.trigger.context;

import com.idealagent.domain.user.model.vo.AuthUserVO;
import com.idealagent.trigger.exception.ForbiddenException;

public final class UserContext {
    private static final ThreadLocal<AuthUserVO> CURRENT = new ThreadLocal<>();

    private UserContext() {
    }

    public static void set(AuthUserVO user) {
        CURRENT.set(user);
    }

    public static AuthUserVO get() {
        return CURRENT.get();
    }

    public static Long userId() {
        AuthUserVO user = get();
        return user == null ? null : user.userId();
    }

    public static String userRole() {
        AuthUserVO user = get();
        return user == null ? null : user.userRole();
    }

    public static boolean isAdmin() {
        return "admin".equalsIgnoreCase(userRole());
    }

    public static void requireAdmin() {
        if (!isAdmin()) {
            throw new ForbiddenException("无权限");
        }
    }

    public static void clear() {
        CURRENT.remove();
    }
}
