package com.ra.base_spring_boot.dto.resp;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.ra.base_spring_boot.model.constants.AlbumType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
@AllArgsConstructor
public class AlbumResponseDTO {
    private Long id;
    private String titleAlbum;
    private String releaseDateStr;
    private Integer artistId;
    private String coverImageUrl;
    private AlbumType type;
    private String status;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String createdAtStr;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String updatedAtStr;
}
