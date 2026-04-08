package com.castorama.atg.dto.response;

import lombok.Builder;
import lombok.Data;

/**
 * Returned on successful login or registration.
 * ATG analogy: the session token and profile data returned after
 * ProfileAuthenticationPipelineServlet succeeds.
 */
@Data
@Builder
public class AuthResponse {
    private String accessToken;
    private String tokenType;
    private long expiresIn;      // seconds
    private UserResponse user;
}
