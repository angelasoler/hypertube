package com.hypertube.user.repository;

import com.hypertube.user.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for PasswordResetToken entity operations
 */
@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {

    /**
     * Find unused, non-expired token by token hash
     */
    @Query("SELECT t FROM PasswordResetToken t WHERE t.tokenHash = :tokenHash AND t.usedAt IS NULL AND t.expiresAt > :now")
    Optional<PasswordResetToken> findValidTokenByHash(
            @Param("tokenHash") String tokenHash,
            @Param("now") Instant now
    );

    /**
     * Find token by token hash (regardless of status)
     */
    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    /**
     * Find unused token by token hash
     */
    Optional<PasswordResetToken> findByTokenHashAndUsedAtIsNull(String tokenHash);

    /**
     * Invalidate (mark as used) all unused tokens for a user
     * This is called when user resets password to invalidate all pending reset requests
     */
    @Modifying
    @Query("UPDATE PasswordResetToken t SET t.usedAt = :now WHERE t.user.id = :userId AND t.usedAt IS NULL")
    void invalidateAllTokensForUser(@Param("userId") UUID userId, @Param("now") Instant now);

    /**
     * Delete expired tokens older than specified date
     * This is for cleanup (typically 7 days after expiration)
     */
    @Modifying
    @Query("DELETE FROM PasswordResetToken t WHERE t.expiresAt < :cutoffDate")
    void deleteExpiredTokensBefore(@Param("cutoffDate") Instant cutoffDate);
}
