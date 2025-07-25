package com.ra.base_spring_boot.dto.resp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Top15AlbumsResponseDTO {
    private Long id;
    private String title;
    private String coverUrl;
    private String artistName;
    private Long totalViews;
}
