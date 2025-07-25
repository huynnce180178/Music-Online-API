package com.ra.base_spring_boot.dto.req;

import com.ra.base_spring_boot.model.constants.AlbumType;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class AlbumRequestDTO {
    @NotBlank(message = "Album title cannot be empty")
    private String titleAlbum;
    private LocalDateTime releaseDate;
    private Long artistId;
    private MultipartFile coverImageUrl;
    private AlbumType type = AlbumType.FREE;
}
