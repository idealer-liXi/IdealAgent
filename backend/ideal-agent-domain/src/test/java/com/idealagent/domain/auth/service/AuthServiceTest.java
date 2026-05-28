package com.idealagent.domain.auth.service;

import com.idealagent.domain.auth.model.dto.LoginDTO;
import com.idealagent.domain.auth.model.dto.RegisterDTO;
import com.idealagent.domain.auth.model.entity.UserAccount;
import com.idealagent.domain.auth.model.vo.AuthTokenVO;
import com.idealagent.domain.auth.model.vo.UserProfileVO;
import com.idealagent.domain.auth.repository.IUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthServiceTest {

    private FakeUserRepository userRepository;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        userRepository = new FakeUserRepository();
        authService = new AuthService(userRepository, new PlainPasswordService(), new StaticTokenService());
    }

    @Test
    void registerCreatesEnabledUserAndReturnsToken() {
        AuthTokenVO token = authService.register(new RegisterDTO("alice", "secret123"));

        UserAccount saved = userRepository.findByUserName("alice").orElseThrow();
        assertThat(saved.getPassword()).isEqualTo("encoded:secret123");
        assertThat(saved.getUserRole()).isEqualTo("user");
        assertThat(saved.getUserStatus()).isEqualTo(1);
        assertThat(token.token()).isEqualTo("token-1-alice-user");
    }

    @Test
    void registerRejectsDuplicateUserName() {
        authService.register(new RegisterDTO("alice", "secret123"));

        assertThatThrownBy(() -> authService.register(new RegisterDTO("alice", "secret123")))
                .isInstanceOf(AuthException.class)
                .hasMessage("用户名已存在");
    }

    @Test
    void loginReturnsTokenWhenPasswordMatches() {
        authService.register(new RegisterDTO("alice", "secret123"));

        AuthTokenVO token = authService.login(new LoginDTO("alice", "secret123"));

        assertThat(token.token()).isEqualTo("token-1-alice-user");
        assertThat(token.profile().userName()).isEqualTo("alice");
    }

    @Test
    void loginRejectsWrongPassword() {
        authService.register(new RegisterDTO("alice", "secret123"));

        assertThatThrownBy(() -> authService.login(new LoginDTO("alice", "bad-password")))
                .isInstanceOf(AuthException.class)
                .hasMessage("用户名或密码错误");
    }

    @Test
    void profileReturnsExistingUser() {
        authService.register(new RegisterDTO("alice", "secret123"));

        UserProfileVO profile = authService.profile(1L);

        assertThat(profile.userName()).isEqualTo("alice");
        assertThat(profile.userRole()).isEqualTo("user");
    }

    private static class FakeUserRepository implements IUserRepository {
        private final Map<Long, UserAccount> usersById = new HashMap<>();
        private final Map<String, UserAccount> usersByName = new HashMap<>();
        private long nextId = 1L;

        @Override
        public Optional<UserAccount> findByUserName(String userName) {
            return Optional.ofNullable(usersByName.get(userName));
        }

        @Override
        public Optional<UserAccount> findById(Long id) {
            return Optional.ofNullable(usersById.get(id));
        }

        @Override
        public UserAccount save(UserAccount userAccount) {
            userAccount.setId(nextId++);
            usersById.put(userAccount.getId(), userAccount);
            usersByName.put(userAccount.getUserName(), userAccount);
            return userAccount;
        }
    }

    private static class PlainPasswordService implements IPasswordService {
        @Override
        public String encode(String rawPassword) {
            return "encoded:" + rawPassword;
        }

        @Override
        public boolean matches(String rawPassword, String encodedPassword) {
            return encode(rawPassword).equals(encodedPassword);
        }
    }

    private static class StaticTokenService implements ITokenService {
        @Override
        public String createToken(UserAccount userAccount) {
            return "token-%d-%s-%s".formatted(userAccount.getId(), userAccount.getUserName(), userAccount.getUserRole());
        }
    }
}
