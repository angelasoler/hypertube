package com.hypertube.user.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * OAuth account entity representing external authentication providers
 * linked to a user account.
 *
 * Supported providers: 42 (mandatory), google, github (optional)
 *
 * Security considerations:
 * - Tokens are encrypted at application layer before storage
 * - Provider + providerUserId must be unique (one external account per provider)
 * - Cascade delete when user is deleted
 */
@Entity
@Table(
    name = "oauth_accounts",
    uniqueConstraints = @UniqueConstraint(
        name = "oauth_unique_account",
        columnNames = {"provider", "provider_user_id"}
    )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OAuthAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * OAuth provider: 42, google, github
     */
    @Column(nullable = false, length = 20)
    private String provider;

    /**
     * User ID from the OAuth provider (e.g., GitHub user ID)
     */
    @Column(name = "provider_user_id", nullable = false)
    private String providerUserId;

    /**
     * OAuth access token
     * IMPORTANT: This should be encrypted at application layer before storage
     */
    @Column(name = "access_token", columnDefinition = "TEXT")
    private String accessToken;

    /**
     * OAuth refresh token
     * IMPORTANT: This should be encrypted at application layer before storage
     */
    @Column(name = "refresh_token", columnDefinition = "TEXT")
    private String refreshToken;

    /**
     * Token expiration timestamp
     */
    @Column(name = "token_expires_at")
    private Instant tokenExpiresAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /**
     * Check if access token is expired
     */
    public boolean isTokenExpired() {
        if (tokenExpiresAt == null) {
            return false; // No expiration set
        }
        return Instant.now().isAfter(tokenExpiresAt);
    }

    /**
     * Check if token needs refresh (expires within 5 minutes)
     */
    public boolean needsTokenRefresh() {
        if (tokenExpiresAt == null) {
            return false;
        }
        Instant refreshThreshold = Instant.now().plusSeconds(300); // 5 minutes
        return tokenExpiresAt.isBefore(refreshThreshold);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OAuthAccount)) return false;
        OAuthAccount that = (OAuthAccount) o;
        return id != null && id.equals(that.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "OAuthAccount{" +
                "id=" + id +
                ", provider='" + provider + '\'' +
                ", providerUserId='" + providerUserId + '\'' +
                ", tokenExpiresAt=" + tokenExpiresAt +
                ", createdAt=" + createdAt +
                '}';
    }
}
