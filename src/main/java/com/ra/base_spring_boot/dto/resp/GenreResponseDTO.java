package com.ra.base_spring_boot.dto.resp;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class GenreResponseDTO {
    private Long id;
    private String genreName;
    private String genreImage;
}
