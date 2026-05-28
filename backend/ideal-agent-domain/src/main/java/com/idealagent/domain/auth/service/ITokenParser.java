package com.idealagent.domain.auth.service;

import com.idealagent.domain.auth.model.vo.AuthUserVO;

public interface ITokenParser {
    AuthUserVO parseToken(String token);
}
