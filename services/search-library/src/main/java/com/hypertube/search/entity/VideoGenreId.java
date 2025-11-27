package com.hypertube.search.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

/**
 * Composite primary key for VideoGenre junction table
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VideoGenreId implements Serializable {
    private UUID video;
    private UUID genre;
}
