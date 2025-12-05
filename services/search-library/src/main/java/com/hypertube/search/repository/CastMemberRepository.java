package com.hypertube.search.repository;

import com.hypertube.search.entity.CastMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for CastMember entity operations
 */
@Repository
public interface CastMemberRepository extends JpaRepository<CastMember, UUID> {

    /**
     * Find cast member by name
     */
    Optional<CastMember> findByName(String name);

    /**
     * Check if cast member exists by name
     */
    boolean existsByName(String name);
}
