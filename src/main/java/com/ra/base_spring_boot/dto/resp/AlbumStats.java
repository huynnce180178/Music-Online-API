package com.ra.base_spring_boot.dto.resp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlbumStats {
    private Integer totalSongs;
    private Integer totalViews;
    private Integer totalDownloads;
    private String totalDuration;
}
