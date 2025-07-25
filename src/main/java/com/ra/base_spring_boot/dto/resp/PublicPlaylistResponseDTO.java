package com.ra.base_spring_boot.dto.resp;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PublicPlaylistResponseDTO {
    private Long id;
    private String name;
    private String description;
    private Boolean isPublic;
    private String imageUrl;
    private String createdAtStr;
    private String updatedAtStr;
    private Long userId;
}

