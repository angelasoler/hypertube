package com.hypertube.search.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for video search request with filters
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SearchRequest {
    private String query;
    private String genre;
    private Integer minYear;
    private Integer maxYear;
    private BigDecimal minRating;
    private Integer page;
    private Integer size;
    private String sortBy; // title, year, rating, seeds, created
    private String sortDirection; // asc, desc
}
