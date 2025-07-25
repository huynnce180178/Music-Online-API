package com.ra.base_spring_boot.dto.req;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class DownloadRequestDTO {
    private Long songId;
    private String downloadPath;
}
