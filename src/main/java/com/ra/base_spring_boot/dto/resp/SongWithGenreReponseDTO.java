package com.ra.base_spring_boot.dto.resp;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@Builder
public class SongWithGenreReponseDTO {
    private Long id;
    private String title;
    private String fileUrl;
    private String profilePicUrl;
}
