package com.idealagent.domain.user.service.auth;

import com.idealagent.domain.user.model.entity.UserAccount;

public interface ITokenService {
    String createToken(UserAccount userAccount);
}
