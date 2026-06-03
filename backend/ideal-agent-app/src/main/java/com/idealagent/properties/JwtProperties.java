package com.idealagent.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ideal-agent.jwt", ignoreInvalidFields = true)
public class JwtProperties {
    private String issuer = "ideal-agent";
    private String secret = "ideal-agent-dev-secret-change-me";
    private long expireHours = 168;

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public long getExpireHours() {
        return expireHours;
    }

    public void setExpireHours(long expireHours) {
        this.expireHours = expireHours;
    }
}
