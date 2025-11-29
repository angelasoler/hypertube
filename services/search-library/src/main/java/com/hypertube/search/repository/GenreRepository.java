package com.hypertube.search.repository;

import com.hypertube.search.entity.Genre;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Genre entity operations
 */
@Repository
public interface GenreRepository extends JpaRepository<Genre, UUID> {

    /**
     * Find genre by name
     */
    Optional<Genre> findByName(String name);

    /**
     * Check if genre exists by name
     */
    boolean existsByName(String name);
}
