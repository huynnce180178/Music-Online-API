package com.ra.base_spring_boot.dto.resp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PlaylistResponseDTO {
    private Long id;
    private String name;
    private String description;
    private Boolean isPublic;
    private String imageUrl;
    private String createdAtStr;
    private String updatedAtStr;
    private Long userId;
    private List<SongResponseDTO> songs;
}
