package com.hypertube.user.repository;

import com.hypertube.user.entity.OAuthAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for OAuthAccount entity operations
 */
@Repository
public interface OAuthAccountRepository extends JpaRepository<OAuthAccount, UUID> {

    /**
     * Find OAuth account by provider and provider user ID
     */
    Optional<OAuthAccount> findByProviderAndProviderUserId(String provider, String providerUserId);

    /**
     * Check if OAuth account exists for provider and provider user ID
     */
    boolean existsByProviderAndProviderUserId(String provider, String providerUserId);

    /**
     * Find OAuth account by user ID and provider
     */
    Optional<OAuthAccount> findByUserIdAndProvider(UUID userId, String provider);
}
