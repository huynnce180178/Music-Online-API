package com.ra.base_spring_boot.dto.resp;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class TopAlbumResponseDTO {
    private Long albumId;
    private String albumName;
    private String albumImage;
    private Long totalViews;
}
