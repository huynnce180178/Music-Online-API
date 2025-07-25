package com.ra.base_spring_boot.dto.resp;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DownloadResponseDTO {
    private Long userId;
    private Long songId;
    private String songTitle;
    private String albumName;
    private String duration;
}
