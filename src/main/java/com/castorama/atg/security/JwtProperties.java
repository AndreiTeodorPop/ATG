package com.castorama.atg.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Externalised JWT configuration bound from {@code application.properties}.
 *
 * <p>ATG analogy: component properties file
 * {@code /atg/dynamo/security/JwtTokenService.properties} in the Nucleus config layer.</p>
 */
@Component
@ConfigurationProperties(prefix = "castorama.jwt")
public class JwtProperties {

    /** HMAC-SHA256 signing secret — must be at least 32 chars in production. */
    private String secret = "castorama-atg-demo-secret-key-change-in-production-32chars";

    /** Token validity in seconds. Default: 24 h. */
    private long expirationSeconds = 86400L;

    public String getSecret() { return secret; }
    public void setSecret(String secret) { this.secret = secret; }

    public long getExpirationSeconds() { return expirationSeconds; }
    public void setExpirationSeconds(long expirationSeconds) {
        this.expirationSeconds = expirationSeconds;
    }
}
