package com.idealagent.api;

import com.idealagent.domain.user.model.dto.LoginDTO;
import com.idealagent.domain.user.model.dto.RegisterDTO;
import com.idealagent.domain.user.model.vo.AuthTokenVO;
import com.idealagent.types.result.Result;

public interface IAuthApi {
    Result<AuthTokenVO> register(RegisterDTO request);

    Result<AuthTokenVO> login(LoginDTO request);
}
