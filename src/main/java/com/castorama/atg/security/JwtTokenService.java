package com.castorama.atg.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Stateless JWT token generation and validation service.
 *
 * <p>ATG analogy: {@code /atg/dynamo/security/http/SessionConfirmationNumber}
 * or a custom Nucleus component managing session tokens.  ATG typically uses
 * cookie-based session affinity; here we use Bearer tokens for stateless REST
 * compatibility — a common pattern when building an API layer on top of ATG.</p>
 *
 * <p>Uses JJWT 0.12.x fluent builder API (not the deprecated 0.9.x
 * {@code Jwts.parser().setSigningKey()} approach).</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class JwtTokenService {

    private final JwtProperties jwtProperties;

    private SecretKey signingKey() {
        return Keys.hmacShaKeyFor(
                jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Generate a signed JWT for the given user.
     *
     * @param userDetails Spring Security principal (login = subject claim)
     * @return compact serialised JWT string
     */
    public String generateToken(UserDetails userDetails) {
        long nowMs = System.currentTimeMillis();
        return Jwts.builder()
                .subject(userDetails.getUsername())
                .claim("roles", userDetails.getAuthorities().toString())
                .issuedAt(new Date(nowMs))
                .expiration(new Date(nowMs + jwtProperties.getExpirationSeconds() * 1_000L))
                .signWith(signingKey(), Jwts.SIG.HS256)
                .compact();
    }

    /**
     * Extract the subject (login/email) from a token without throwing.
     *
     * @return subject string, or null if the token is invalid
     */
    public String extractSubject(String token) {
        try {
            return parseClaims(token).getPayload().getSubject();
        } catch (JwtException | IllegalArgumentException ex) {
            log.debug("JWT subject extraction failed: {}", ex.getMessage());
            return null;
        }
    }

    /**
     * Validate token integrity and expiry against the given UserDetails.
     *
     * @param token       JWT string from Authorization header
     * @param userDetails authenticated principal
     * @return true if token is valid and matches the principal
     */
    public boolean isValid(String token, UserDetails userDetails) {
        try {
            Jws<Claims> jws = parseClaims(token);
            String subject = jws.getPayload().getSubject();
            return subject.equals(userDetails.getUsername())
                    && !jws.getPayload().getExpiration().before(new Date());
        } catch (JwtException | IllegalArgumentException ex) {
            log.debug("JWT validation failed: {}", ex.getMessage());
            return false;
        }
    }

    public long getExpirationSeconds() {
        return jwtProperties.getExpirationSeconds();
    }

    private Jws<Claims> parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey())
                .build()
                .parseSignedClaims(token);
    }
}
