package com.idealagent.domain.user.service.auth;

import com.idealagent.domain.user.model.vo.AuthUserVO;

public interface ITokenParser {
    AuthUserVO parseToken(String token);
}
