package com.hypertube.user.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * User entity representing a registered user in the system.
 *
 * Security considerations:
 * - Password is stored as bcrypt hash (never plain text)
 * - Password hash is nullable for OAuth-only users
 * - Email must be unique and verified for password reset
 * - Username must be unique and alphanumeric
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 30)
    private String username;

    @Column(nullable = false, unique = true)
    private String email;

    /**
     * Bcrypt password hash with salt (cost factor: 12)
     * Nullable for OAuth-only users who don't have a password
     */
    @Column(name = "password_hash", length = 60)
    private String passwordHash;

    @Column(name = "profile_picture_url", length = 500)
    private String profilePictureUrl;

    @Column(name = "preferred_language", nullable = false, length = 5)
    @Builder.Default
    private String preferredLanguage = "en";

    @Column(name = "is_email_verified", nullable = false)
    @Builder.Default
    private Boolean isEmailVerified = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    /**
     * OAuth accounts linked to this user
     * CascadeType.ALL ensures OAuth accounts are deleted when user is deleted
     */
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private Set<OAuthAccount> oauthAccounts = new HashSet<>();

    /**
     * Password reset tokens for this user
     * CascadeType.ALL ensures tokens are deleted when user is deleted
     */
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private Set<PasswordResetToken> passwordResetTokens = new HashSet<>();

    /**
     * Helper method to add an OAuth account
     */
    public void addOAuthAccount(OAuthAccount oauthAccount) {
        oauthAccounts.add(oauthAccount);
        oauthAccount.setUser(this);
    }

    /**
     * Helper method to remove an OAuth account
     */
    public void removeOAuthAccount(OAuthAccount oauthAccount) {
        oauthAccounts.remove(oauthAccount);
        oauthAccount.setUser(null);
    }

    /**
     * Check if user has a password (not OAuth-only)
     */
    public boolean hasPassword() {
        return passwordHash != null && !passwordHash.isEmpty();
    }

    /**
     * Check if user has any OAuth accounts
     */
    public boolean hasOAuthAccounts() {
        return !oauthAccounts.isEmpty();
    }

    /**
     * Get OAuth account by provider
     */
    public OAuthAccount getOAuthAccount(String provider) {
        return oauthAccounts.stream()
                .filter(account -> account.getProvider().equalsIgnoreCase(provider))
                .findFirst()
                .orElse(null);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User)) return false;
        User user = (User) o;
        return id != null && id.equals(user.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", preferredLanguage='" + preferredLanguage + '\'' +
                ", isEmailVerified=" + isEmailVerified +
                ", createdAt=" + createdAt +
                '}';
    }
}
