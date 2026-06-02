package com.idealagent.trigger.context;

import com.idealagent.domain.user.model.vo.AuthUserVO;

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

    public static void clear() {
        CURRENT.remove();
    }
}
