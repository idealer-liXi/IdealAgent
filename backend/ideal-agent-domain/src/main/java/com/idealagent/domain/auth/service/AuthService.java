package com.idealagent.domain.auth.service;

import com.idealagent.domain.auth.model.dto.LoginDTO;
import com.idealagent.domain.auth.model.dto.RegisterDTO;
import com.idealagent.domain.auth.model.entity.UserAccount;
import com.idealagent.domain.auth.model.vo.AuthTokenVO;
import com.idealagent.domain.auth.model.vo.UserProfileVO;
import com.idealagent.domain.auth.repository.IUserRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AuthService {
    private static final String DEFAULT_ROLE = "user";
    private static final int ENABLED = 1;

    private final IUserRepository userRepository;
    private final IPasswordService passwordService;
    private final ITokenService tokenService;

    public AuthService(IUserRepository userRepository, IPasswordService passwordService, ITokenService tokenService) {
        this.userRepository = userRepository;
        this.passwordService = passwordService;
        this.tokenService = tokenService;
    }

    public AuthTokenVO register(RegisterDTO request) {
        validateUserNameAndPassword(request.userName(), request.password());
        if (userRepository.findByUserName(request.userName()).isPresent()) {
            throw new AuthException("用户名已存在");
        }

        UserAccount userAccount = new UserAccount();
        userAccount.setUserName(request.userName());
        userAccount.setPassword(passwordService.encode(request.password()));
        userAccount.setUserRole(DEFAULT_ROLE);
        userAccount.setUserStatus(ENABLED);

        UserAccount saved = userRepository.save(userAccount);
        return tokenFor(saved);
    }

    public AuthTokenVO login(LoginDTO request) {
        validateUserNameAndPassword(request.userName(), request.password());
        UserAccount userAccount = userRepository.findByUserName(request.userName())
                .filter(user -> ENABLED == user.getUserStatus())
                .orElseThrow(() -> new AuthException("用户名或密码错误"));

        if (!passwordService.matches(request.password(), userAccount.getPassword())) {
            throw new AuthException("用户名或密码错误");
        }

        return tokenFor(userAccount);
    }

    public UserProfileVO profile(Long userId) {
        UserAccount userAccount = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException("用户不存在"));
        return profileOf(userAccount);
    }

    private AuthTokenVO tokenFor(UserAccount userAccount) {
        return new AuthTokenVO(tokenService.createToken(userAccount), profileOf(userAccount));
    }

    private UserProfileVO profileOf(UserAccount userAccount) {
        return new UserProfileVO(userAccount.getId(), userAccount.getUserName(), userAccount.getUserRole(), userAccount.getUserAvatar());
    }

    private void validateUserNameAndPassword(String userName, String password) {
        if (!StringUtils.hasText(userName) || !StringUtils.hasText(password)) {
            throw new AuthException("用户名和密码不能为空");
        }
        if (password.length() < 6) {
            throw new AuthException("密码长度不能少于6位");
        }
    }
}
