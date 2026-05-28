package com.idealagent.infrastructure.repository;

import com.idealagent.domain.auth.model.entity.UserAccount;
import com.idealagent.domain.auth.repository.IUserRepository;
import com.idealagent.infrastructure.persistent.dao.IAiUserDao;
import com.idealagent.infrastructure.persistent.po.AiUser;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class UserRepository implements IUserRepository {
    private final IAiUserDao aiUserDao;

    public UserRepository(IAiUserDao aiUserDao) {
        this.aiUserDao = aiUserDao;
    }

    @Override
    public Optional<UserAccount> findByUserName(String userName) {
        return Optional.ofNullable(toDomain(aiUserDao.queryByUserName(userName)));
    }

    @Override
    public Optional<UserAccount> findById(Long id) {
        return Optional.ofNullable(toDomain(aiUserDao.queryById(id)));
    }

    @Override
    public UserAccount save(UserAccount userAccount) {
        AiUser aiUser = toPo(userAccount);
        aiUserDao.insert(aiUser);
        userAccount.setId(aiUser.getId());
        return userAccount;
    }

    private UserAccount toDomain(AiUser aiUser) {
        if (aiUser == null) {
            return null;
        }
        UserAccount userAccount = new UserAccount();
        userAccount.setId(aiUser.getId());
        userAccount.setUserName(aiUser.getUserName());
        userAccount.setPassword(aiUser.getPassword());
        userAccount.setUserRole(aiUser.getUserRole());
        userAccount.setUserAvatar(aiUser.getUserAvatar());
        userAccount.setUserStatus(aiUser.getUserStatus());
        userAccount.setCreateTime(aiUser.getCreateTime());
        userAccount.setUpdateTime(aiUser.getUpdateTime());
        return userAccount;
    }

    private AiUser toPo(UserAccount userAccount) {
        AiUser aiUser = new AiUser();
        aiUser.setId(userAccount.getId());
        aiUser.setUserName(userAccount.getUserName());
        aiUser.setPassword(userAccount.getPassword());
        aiUser.setUserRole(userAccount.getUserRole());
        aiUser.setUserAvatar(userAccount.getUserAvatar());
        aiUser.setUserStatus(userAccount.getUserStatus());
        return aiUser;
    }
}
