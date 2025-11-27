package com.hypertube.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * DTO for user information response
 * Does NOT include sensitive data like password hash
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponse {

    private UUID id;
    private String username;
    private String email;
    private String profilePictureUrl;
    private String preferredLanguage;
    private Boolean isEmailVerified;
    private Instant createdAt;
    private Instant lastLoginAt;
    private List<String> oauthProviders; // e.g., ["42", "google"]
}
