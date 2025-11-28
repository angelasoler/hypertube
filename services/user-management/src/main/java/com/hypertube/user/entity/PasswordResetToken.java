package com.hypertube.user.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Password reset token entity for forgot password flow.
 *
 * Security considerations:
 * - Token is stored as SHA-256 hash (never plain text)
 * - Tokens expire after 15 minutes
 * - Tokens are single-use (tracked via usedAt)
 * - Old tokens are cleaned up automatically (see Flyway migration)
 */
@Entity
@Table(name = "password_reset_tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * SHA-256 hash of the reset token
     * The actual token is sent to user's email and never stored
     */
    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    /**
     * Token expiration timestamp (15 minutes from creation)
     */
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    /**
     * Timestamp when token was used (NULL if unused)
     * Once used, token cannot be reused
     */
    @Column(name = "used_at")
    private Instant usedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * Check if token is expired
     */
    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    /**
     * Check if token has been used
     */
    public boolean isUsed() {
        return usedAt != null;
    }

    /**
     * Check if token is valid (not expired and not used)
     */
    public boolean isValid() {
        return !isExpired() && !isUsed();
    }

    /**
     * Mark token as used
     */
    public void markAsUsed() {
        this.usedAt = Instant.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PasswordResetToken)) return false;
        PasswordResetToken that = (PasswordResetToken) o;
        return id != null && id.equals(that.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "PasswordResetToken{" +
                "id=" + id +
                ", tokenHash='" + tokenHash.substring(0, 8) + "...'" + // Only show first 8 chars
                ", expiresAt=" + expiresAt +
                ", usedAt=" + usedAt +
                ", createdAt=" + createdAt +
                '}';
    }
}
