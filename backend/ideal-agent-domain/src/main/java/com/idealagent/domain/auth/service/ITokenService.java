package com.idealagent.domain.auth.service;

import com.idealagent.domain.auth.model.entity.UserAccount;

public interface ITokenService {
    String createToken(UserAccount userAccount);
}
