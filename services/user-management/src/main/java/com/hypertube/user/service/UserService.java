package com.hypertube.user.service;

import com.hypertube.user.dto.AuthResponse;
import com.hypertube.user.dto.LoginRequest;
import com.hypertube.user.dto.RegisterRequest;
import com.hypertube.user.dto.UserResponse;
import com.hypertube.user.entity.PasswordResetToken;
import com.hypertube.user.entity.User;
import com.hypertube.user.mapper.UserMapper;
import com.hypertube.user.repository.PasswordResetTokenRepository;
import com.hypertube.user.repository.UserRepository;
import com.hypertube.user.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

/**
 * Service for user management operations
 * Handles registration, authentication, password management
 *
 * Security features:
 * - Bcrypt password hashing with cost factor 12
 * - Secure random token generation for password reset
 * - SHA-256 hashing of reset tokens before storage
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final EmailService emailService;

    // BCrypt with cost factor 12 (recommended for 2024+)
    private static final BCryptPasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder(12);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int RESET_TOKEN_EXPIRATION_MINUTES = 15;

    /**
     * Registers a new user with username, email, and password
     *
     * @param request Registration request with user details
     * @return Authentication response with JWT tokens
     * @throws IllegalArgumentException if username or email already exists
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        log.info("Attempting to register user: {}", request.getUsername());

        // Check if username already exists
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username already exists");
        }

        // Check if email already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }

        // Create new user
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPasswordHash(PASSWORD_ENCODER.encode(request.getPassword()));
        user.setPreferredLanguage(request.getPreferredLanguage() != null
            ? request.getPreferredLanguage()
            : "en");
        user.setIsEmailVerified(false);
        user.setCreatedAt(Instant.now());
        user.setUpdatedAt(Instant.now());

        User savedUser = userRepository.save(user);
        log.info("User registered successfully: {}", savedUser.getUsername());

        // TODO: Send email verification email
        // emailService.sendEmailVerificationEmail(savedUser.getEmail(), verificationToken);

        // Generate JWT tokens
        String accessToken = jwtUtil.generateAccessToken(savedUser.getId(), savedUser.getUsername());
        String refreshToken = jwtUtil.generateRefreshToken(savedUser.getId());

        return AuthResponse.builder()
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .tokenType("Bearer")
            .expiresIn(3600L) // 1 hour
            .user(userMapper.toUserResponse(savedUser))
            .build();
    }

    /**
     * Authenticates a user with username/email and password
     *
     * @param request Login request with credentials
     * @return Authentication response with JWT tokens
     * @throws IllegalArgumentException if credentials are invalid
     */
    @Transactional
    public AuthResponse login(LoginRequest request) {
        log.info("Attempting login for: {}", request.getUsernameOrEmail());

        // Find user by username or email
        User user = userRepository.findByUsernameOrEmail(request.getUsernameOrEmail())
            .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));

        // Check if user has a password (OAuth-only users don't)
        if (!user.hasPassword()) {
            throw new IllegalArgumentException("This account uses OAuth authentication only");
        }

        // Verify password
        if (!PASSWORD_ENCODER.matches(request.getPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid credentials");
        }

        // Update last login timestamp
        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        log.info("User logged in successfully: {}", user.getUsername());

        // Generate JWT tokens
        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getUsername());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId());

        return AuthResponse.builder()
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .tokenType("Bearer")
            .expiresIn(3600L) // 1 hour
            .user(userMapper.toUserResponse(user))
            .build();
    }

    /**
     * Initiates password reset flow by generating a reset token and sending email
     *
     * @param email User's email address
     */
    @Transactional
    public void requestPasswordReset(String email) {
        log.info("Password reset requested for email: {}", email);

        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new IllegalArgumentException("Email not found"));

        // Check if user has a password (OAuth-only users can't reset)
        if (!user.hasPassword()) {
            throw new IllegalArgumentException("This account uses OAuth authentication only");
        }

        // Generate secure random token (32 bytes = 256 bits)
        byte[] tokenBytes = new byte[32];
        SECURE_RANDOM.nextBytes(tokenBytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);

        // Hash the token before storing (SHA-256)
        String tokenHash = hashToken(rawToken);

        // Create password reset token
        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setUser(user);
        resetToken.setTokenHash(tokenHash);
        resetToken.setExpiresAt(Instant.now().plusSeconds(RESET_TOKEN_EXPIRATION_MINUTES * 60));
        resetToken.setCreatedAt(Instant.now());

        passwordResetTokenRepository.save(resetToken);

        // Send reset email with raw token (not the hash)
        emailService.sendPasswordResetEmail(user.getEmail(), rawToken);

        log.info("Password reset token created for user: {}", user.getUsername());
    }

    /**
     * Confirms password reset with token and new password
     *
     * @param rawToken Password reset token from email
     * @param newPassword New password
     * @throws IllegalArgumentException if token is invalid or expired
     */
    @Transactional
    public void confirmPasswordReset(String rawToken, String newPassword) {
        log.info("Attempting to confirm password reset");

        // Hash the provided token to look it up
        String tokenHash = hashToken(rawToken);

        PasswordResetToken resetToken = passwordResetTokenRepository
            .findByTokenHashAndUsedAtIsNull(tokenHash)
            .orElseThrow(() -> new IllegalArgumentException("Invalid or expired reset token"));

        // Validate token
        if (!resetToken.isValid()) {
            throw new IllegalArgumentException("Invalid or expired reset token");
        }

        // Update user password
        User user = resetToken.getUser();
        user.setPasswordHash(PASSWORD_ENCODER.encode(newPassword));
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);

        // Mark token as used
        resetToken.markAsUsed();
        passwordResetTokenRepository.save(resetToken);

        log.info("Password reset successful for user: {}", user.getUsername());
    }

    /**
     * Retrieves user profile by ID
     *
     * @param userId User's unique identifier
     * @return User profile information
     * @throws IllegalArgumentException if user not found
     */
    @Transactional(readOnly = true)
    public UserResponse getUserProfile(UUID userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));

        return userMapper.toUserResponse(user);
    }

    /**
     * Updates user profile information
     *
     * @param userId User's unique identifier
     * @param email New email (optional)
     * @param profilePictureUrl New profile picture URL (optional)
     * @param preferredLanguage New preferred language (optional)
     * @return Updated user profile
     */
    @Transactional
    public UserResponse updateUserProfile(
        UUID userId,
        String email,
        String profilePictureUrl,
        String preferredLanguage
    ) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));

        boolean updated = false;

        // Update email if provided and different
        if (email != null && !email.equals(user.getEmail())) {
            if (userRepository.existsByEmail(email)) {
                throw new IllegalArgumentException("Email already exists");
            }
            user.setEmail(email);
            user.setIsEmailVerified(false); // Require re-verification
            updated = true;
        }

        // Update profile picture URL if provided
        if (profilePictureUrl != null && !profilePictureUrl.equals(user.getProfilePictureUrl())) {
            user.setProfilePictureUrl(profilePictureUrl);
            updated = true;
        }

        // Update preferred language if provided
        if (preferredLanguage != null && !preferredLanguage.equals(user.getPreferredLanguage())) {
            user.setPreferredLanguage(preferredLanguage);
            updated = true;
        }

        if (updated) {
            user.setUpdatedAt(Instant.now());
            userRepository.save(user);
            log.info("User profile updated: {}", user.getUsername());
        }

        return userMapper.toUserResponse(user);
    }

    /**
     * Hashes a token using SHA-256
     *
     * @param rawToken Raw token string
     * @return Hexadecimal hash string
     */
    private String hashToken(String rawToken) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(java.nio.charset.StandardCharsets.UTF_8));

            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
}
