package com.idealagent.infrastructure.auth;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.idealagent.domain.auth.model.entity.UserAccount;
import com.idealagent.domain.auth.model.vo.AuthUserVO;
import com.idealagent.domain.auth.service.AuthException;
import com.idealagent.domain.auth.service.ITokenParser;
import com.idealagent.domain.auth.service.ITokenService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
public class JwtUtil implements ITokenService, ITokenParser {
    private final String issuer;
    private final long expireHours;
    private final Algorithm algorithm;
    private final JWTVerifier verifier;

    public JwtUtil(
            @Value("${ideal-agent.jwt.issuer:ideal-agent}") String issuer,
            @Value("${ideal-agent.jwt.secret:ideal-agent-dev-secret-change-me}") String secret,
            @Value("${ideal-agent.jwt.expire-hours:168}") long expireHours) {
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
