package com.hypertube.user.service;

import com.hypertube.user.dto.AuthResponse;
import com.hypertube.user.dto.UserResponse;
import com.hypertube.user.entity.OAuthAccount;
import com.hypertube.user.entity.User;
import com.hypertube.user.mapper.UserMapper;
import com.hypertube.user.repository.OAuthAccountRepository;
import com.hypertube.user.repository.UserRepository;
import com.hypertube.user.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;

/**
 * Service for OAuth2 authentication flow
 * Handles 42 school, Google, and GitHub OAuth providers
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OAuth2Service {

    private final UserRepository userRepository;
    private final OAuthAccountRepository oauthAccountRepository;
    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final OAuth2AuthorizedClientService authorizedClientService;

    /**
     * Processes OAuth2 login/registration callback
     * Creates new user if doesn't exist, links OAuth account if needed
     *
     * @param oauth2User OAuth2 user information from provider
     * @param provider OAuth provider name (42, google, github)
     * @return Authentication response with JWT tokens
     */
    @Transactional
    public AuthResponse processOAuth2Login(OAuth2User oauth2User, String provider) {
        log.info("Processing OAuth2 login for provider: {}", provider);

        // Extract provider-specific user information
        String providerUserId = extractProviderUserId(oauth2User, provider);
        String email = extractEmail(oauth2User, provider);
        String username = extractUsername(oauth2User, provider);
        String profilePictureUrl = extractProfilePictureUrl(oauth2User, provider);

        // Check if OAuth account already exists
        OAuthAccount existingOAuthAccount = oauthAccountRepository
            .findByProviderAndProviderUserId(provider, providerUserId)
            .orElse(null);

        User user;

        if (existingOAuthAccount != null) {
            // Existing OAuth account - log in
            user = existingOAuthAccount.getUser();
            log.info("Existing OAuth account found for user: {}", user.getUsername());

            // Update last login
            user.setLastLoginAt(Instant.now());
            userRepository.save(user);
        } else {
            // New OAuth account - check if user exists by email
            user = userRepository.findByEmail(email).orElse(null);

            if (user == null) {
                // Create new user
                user = createUserFromOAuth(email, username, profilePictureUrl);
                log.info("Created new user from OAuth: {}", user.getUsername());
            }

            // Link OAuth account to user
            OAuthAccount oauthAccount = new OAuthAccount();
            oauthAccount.setUser(user);
            oauthAccount.setProvider(provider);
            oauthAccount.setProviderUserId(providerUserId);
            oauthAccount.setCreatedAt(Instant.now());
            oauthAccount.setUpdatedAt(Instant.now());

            oauthAccountRepository.save(oauthAccount);
            log.info("Linked OAuth account ({}) to user: {}", provider, user.getUsername());
        }

        // Generate JWT tokens
        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getUsername());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId());

        return AuthResponse.builder()
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .tokenType("Bearer")
            .expiresIn(3600L)
            .user(userMapper.toUserResponse(user))
            .build();
    }

    /**
     * Creates a new user from OAuth information
     */
    private User createUserFromOAuth(String email, String username, String profilePictureUrl) {
        User user = new User();
        user.setEmail(email);
        user.setUsername(ensureUniqueUsername(username));
        user.setProfilePictureUrl(profilePictureUrl);
        user.setPreferredLanguage("en");
        user.setIsEmailVerified(true); // OAuth providers verify emails
        user.setPasswordHash(null); // OAuth-only user (no password)
        user.setCreatedAt(Instant.now());
        user.setUpdatedAt(Instant.now());
        user.setLastLoginAt(Instant.now());

        return userRepository.save(user);
    }

    /**
     * Ensures username is unique by appending numbers if needed
     */
    private String ensureUniqueUsername(String baseUsername) {
        String username = baseUsername;
        int suffix = 1;

        while (userRepository.existsByUsername(username)) {
            username = baseUsername + suffix;
            suffix++;
        }

        return username;
    }

    /**
     * Extracts provider user ID from OAuth2User attributes
     */
    private String extractProviderUserId(OAuth2User oauth2User, String provider) {
        return switch (provider) {
            case "42" -> oauth2User.getAttribute("id").toString();
            case "google" -> oauth2User.getAttribute("sub");
            case "github" -> oauth2User.getAttribute("id").toString();
            default -> throw new IllegalArgumentException("Unsupported OAuth provider: " + provider);
        };
    }

    /**
     * Extracts email from OAuth2User attributes
     */
    private String extractEmail(OAuth2User oauth2User, String provider) {
        String email = oauth2User.getAttribute("email");
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email is required from OAuth provider");
        }
        return email;
    }

    /**
     * Extracts username from OAuth2User attributes
     */
    private String extractUsername(OAuth2User oauth2User, String provider) {
        return switch (provider) {
            case "42" -> {
                String login = oauth2User.getAttribute("login");
                yield login != null ? login : "user42";
            }
            case "google" -> {
                String name = oauth2User.getAttribute("name");
                yield name != null ? name.replaceAll("[^a-zA-Z0-9_]", "_") : "googleuser";
            }
            case "github" -> {
                String login = oauth2User.getAttribute("login");
                yield login != null ? login : "githubuser";
            }
            default -> "user";
        };
    }

    /**
     * Extracts profile picture URL from OAuth2User attributes
     */
    private String extractProfilePictureUrl(OAuth2User oauth2User, String provider) {
        return switch (provider) {
            case "42" -> {
                Map<String, Object> image = oauth2User.getAttribute("image");
                yield image != null ? (String) image.get("link") : null;
            }
            case "google" -> oauth2User.getAttribute("picture");
            case "github" -> oauth2User.getAttribute("avatar_url");
            default -> null;
        };
    }
}
