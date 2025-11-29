package com.hypertube.user.controller;

import com.hypertube.user.dto.AuthResponse;
import com.hypertube.user.dto.LoginRequest;
import com.hypertube.user.dto.RegisterRequest;
import com.hypertube.user.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller for authentication endpoints
 * Handles registration, login, and password reset flows
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Validated
public class AuthController {

    private final UserService userService;

    /**
     * POST /api/auth/register
     * Registers a new user account
     *
     * @param request Registration details
     * @return Authentication response with JWT tokens
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        try {
            AuthResponse response = userService.register(request);
            log.info("Registration successful for username: {}", request.getUsername());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            log.warn("Registration failed: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * POST /api/auth/login
     * Authenticates a user with username/email and password
     *
     * @param request Login credentials
     * @return Authentication response with JWT tokens
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        try {
            AuthResponse response = userService.login(request);
            log.info("Login successful for: {}", request.getUsernameOrEmail());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("Login failed: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * POST /api/auth/forgot-password
     * Initiates password reset flow by sending reset email
     *
     * @param request Forgot password request with email
     * @return Success message
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(
        @Valid @RequestBody ForgotPasswordRequest request
    ) {
        try {
            userService.requestPasswordReset(request.getEmail());
            log.info("Password reset requested for email: {}", request.getEmail());

            // Always return success to prevent email enumeration
            return ResponseEntity.ok(Map.of(
                "message", "If the email exists, a password reset link has been sent"
            ));
        } catch (Exception e) {
            log.error("Password reset request failed", e);
            // Still return success to prevent email enumeration
            return ResponseEntity.ok(Map.of(
                "message", "If the email exists, a password reset link has been sent"
            ));
        }
    }

    /**
     * POST /api/auth/reset-password
     * Confirms password reset with token and new password
     *
     * @param request Reset password request with token and new password
     * @return Success message
     */
    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(
        @Valid @RequestBody ResetPasswordRequest request
    ) {
        try {
            userService.confirmPasswordReset(request.getToken(), request.getNewPassword());
            log.info("Password reset successful");
            return ResponseEntity.ok(Map.of("message", "Password reset successful"));
        } catch (IllegalArgumentException e) {
            log.warn("Password reset failed: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * DTO for forgot password request
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ForgotPasswordRequest {
        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        private String email;
    }

    /**
     * DTO for reset password request
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResetPasswordRequest {
        @NotBlank(message = "Token is required")
        private String token;

        @NotBlank(message = "New password is required")
        @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
        @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).*$",
            message = "Password must contain at least one uppercase letter, one lowercase letter, and one digit"
        )
        private String newPassword;
    }
}
