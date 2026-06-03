package com.idealagent.domain.user.service.auth;

public interface IPasswordService {
    String encode(String rawPassword);

    boolean matches(String rawPassword, String encodedPassword);
}
