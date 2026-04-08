package com.castorama.atg.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Safe public representation of a User profile.
 * Never exposes passwordHash — ATG equivalent of a profile view
 * with sensitive properties excluded.
 */
@Data
@Builder
public class UserResponse {
    private Long id;
    private String login;
    private String email;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private String role;
    private LocalDateTime createdAt;
}
