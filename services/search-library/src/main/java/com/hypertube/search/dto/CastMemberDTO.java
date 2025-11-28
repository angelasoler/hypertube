package com.hypertube.search.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for CastMember response
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CastMemberDTO {
    private String name;
    private String role;
    private String characterName;
    private String profilePhotoUrl;
}
