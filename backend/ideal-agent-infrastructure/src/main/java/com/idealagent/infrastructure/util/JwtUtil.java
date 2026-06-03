package com.idealagent.infrastructure.util;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.idealagent.domain.user.model.entity.UserAccount;
import com.idealagent.domain.user.model.vo.AuthUserVO;
import com.idealagent.domain.user.service.auth.AuthException;
import com.idealagent.domain.user.service.auth.ITokenParser;
import com.idealagent.domain.user.service.auth.ITokenService;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class JwtUtil implements ITokenService, ITokenParser {
    private final String issuer;
    private final long expireHours;
    private final Algorithm algorithm;
    private final JWTVerifier verifier;

    public JwtUtil(String issuer, String secret, long expireHours) {
        this.issuer = issuer;
        this.expireHours = expireHours;
        this.algorithm = Algorithm.HMAC256(secret);
        this.verifier = JWT.require(algorithm).withIssuer(issuer).build();
    }

    @Override
    public String createToken(UserAccount userAccount) {
        return JWT.create()
                .withIssuer(issuer)
                .withSubject(String.valueOf(userAccount.getId()))
                .withClaim("userName", userAccount.getUserName())
                .withClaim("userRole", userAccount.getUserRole())
                .withExpiresAt(Instant.now().plus(expireHours, ChronoUnit.HOURS))
                .sign(algorithm);
    }

    @Override
    public AuthUserVO parseToken(String token) {
        try {
            DecodedJWT jwt = verifier.verify(token);
            return new AuthUserVO(
                    Long.valueOf(jwt.getSubject()),
                    jwt.getClaim("userName").asString(),
                    jwt.getClaim("userRole").asString());
        } catch (Exception e) {
            throw new AuthException("登录状态无效");
        }
    }
}
