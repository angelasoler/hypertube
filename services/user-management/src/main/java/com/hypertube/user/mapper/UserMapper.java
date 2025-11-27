package com.hypertube.user.mapper;

import com.hypertube.user.dto.UserResponse;
import com.hypertube.user.entity.OAuthAccount;
import com.hypertube.user.entity.User;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

/**
 * Maps User entity to DTOs
 * Ensures sensitive data (password hash) is never exposed
 */
@Component
public class UserMapper {

    /**
     * Converts User entity to UserResponse DTO
     * Excludes sensitive information like password hash
     *
     * @param user User entity
     * @return UserResponse DTO
     */
    public UserResponse toUserResponse(User user) {
        if (user == null) {
            return null;
        }

        return UserResponse.builder()
            .id(user.getId())
            .username(user.getUsername())
            .email(user.getEmail())
            .profilePictureUrl(user.getProfilePictureUrl())
            .preferredLanguage(user.getPreferredLanguage())
            .isEmailVerified(user.getIsEmailVerified())
            .createdAt(user.getCreatedAt())
            .lastLoginAt(user.getLastLoginAt())
            .oauthProviders(
                user.getOauthAccounts() != null
                    ? user.getOauthAccounts().stream()
                        .map(OAuthAccount::getProvider)
                        .collect(Collectors.toList())
                    : null
            )
            .build();
    }
}
