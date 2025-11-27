package com.hypertube.user.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for user registration request
 *
 * Security validations:
 * - Username: 3-30 chars, alphanumeric + underscore only
 * - Email: Valid email format
 * - Password: Minimum 8 chars, must contain uppercase, lowercase, and digit
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterRequest {

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 30, message = "Username must be between 3 and 30 characters")
    @Pattern(
        regexp = "^[a-zA-Z0-9_]+$",
        message = "Username must contain only alphanumeric characters and underscores"
    )
    private String username;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).*$",
        message = "Password must contain at least one uppercase letter, one lowercase letter, and one digit"
    )
    private String password;

    @Pattern(
        regexp = "^[a-z]{2}(-[A-Z]{2})?$",
        message = "Preferred language must be a valid ISO 639-1 code (e.g., en, fr, es, en-US)"
    )
    private String preferredLanguage;
}
