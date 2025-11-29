package com.hypertube.user.controller;

import com.hypertube.user.dto.UserResponse;
import com.hypertube.user.service.UserService;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST controller for user profile management
 * Protected endpoints requiring authentication
 */
@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Validated
public class UserController {

    private final UserService userService;

    /**
     * GET /api/users/me
     * Retrieves the authenticated user's profile
     *
     * @param authentication Spring Security authentication object
     * @return User profile information
     */
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUserProfile(Authentication authentication) {
        // Extract user ID from JWT claims (set by API Gateway)
        UUID userId = extractUserIdFromAuth(authentication);

        UserResponse user = userService.getUserProfile(userId);
        log.info("Profile retrieved for user: {}", userId);
        return ResponseEntity.ok(user);
    }

    /**
     * PATCH /api/users/me
     * Updates the authenticated user's profile
     *
     * @param authentication Spring Security authentication object
     * @param request Update request with optional fields
     * @return Updated user profile
     */
    @PatchMapping("/me")
    public ResponseEntity<UserResponse> updateCurrentUserProfile(
        Authentication authentication,
        @RequestBody @Validated UpdateProfileRequest request
    ) {
        UUID userId = extractUserIdFromAuth(authentication);

        UserResponse updatedUser = userService.updateUserProfile(
            userId,
            request.getEmail(),
            request.getProfilePictureUrl(),
            request.getPreferredLanguage()
        );

        log.info("Profile updated for user: {}", userId);
        return ResponseEntity.ok(updatedUser);
    }

    /**
     * Extracts user ID from Spring Security authentication
     * The API Gateway should set this in the JWT claims
     *
     * @param authentication Spring Security authentication
     * @return User ID
     */
    private UUID extractUserIdFromAuth(Authentication authentication) {
        // For now, we'll get it from the principal name (subject claim)
        // In production, the API Gateway would extract and forward this
        String userIdStr = authentication.getName();
        return UUID.fromString(userIdStr);
    }

    /**
     * DTO for profile update request
     * All fields are optional - only provided fields will be updated
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateProfileRequest {

        @Email(message = "Email must be valid")
        private String email;

        private String profilePictureUrl;

        @Pattern(
            regexp = "^[a-z]{2}(-[A-Z]{2})?$",
            message = "Preferred language must be a valid ISO 639-1 code (e.g., en, fr, es, en-US)"
        )
        private String preferredLanguage;
    }
}
