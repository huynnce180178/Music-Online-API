package com.ra.base_spring_boot.dto.req;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.springframework.web.multipart.MultipartFile;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class GenreRequestDTO {
    @NotBlank(message = "Song genre name cannot be empty")
    private String genreName;
    private MultipartFile genreImage;

}
