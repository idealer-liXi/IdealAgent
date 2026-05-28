package com.idealagent.domain.auth.repository;

import com.idealagent.domain.auth.model.entity.UserAccount;

import java.util.Optional;

public interface IUserRepository {
    Optional<UserAccount> findByUserName(String userName);

    Optional<UserAccount> findById(Long id);

    UserAccount save(UserAccount userAccount);
}
