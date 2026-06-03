package com.idealagent.api;

import com.idealagent.domain.user.model.vo.UserProfileVO;
import com.idealagent.types.result.Result;

public interface IUserApi {
    Result<UserProfileVO> profile();
}
