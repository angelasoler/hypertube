package com.hypertube.search.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Generic paged response wrapper for infinite scroll
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PagedResponse<T> {
    private List<T> content;
    private Integer page;
    private Integer size;

    @JsonProperty("total")  // Frontend expects "total" instead of "totalElements"
    private Long totalElements;

    private Integer totalPages;
    private Boolean last;
}
