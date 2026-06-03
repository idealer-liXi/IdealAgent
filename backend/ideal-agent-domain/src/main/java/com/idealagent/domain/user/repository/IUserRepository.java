package com.idealagent.domain.user.repository;

import com.idealagent.domain.user.model.entity.UserAccount;

import java.util.Optional;

public interface IUserRepository {
    Optional<UserAccount> findByUserName(String userName);

    Optional<UserAccount> findById(Long id);

    UserAccount save(UserAccount userAccount);
}
