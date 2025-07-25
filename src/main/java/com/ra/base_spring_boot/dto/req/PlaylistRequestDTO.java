package com.ra.base_spring_boot.dto.req;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class PlaylistRequestDTO {
    @NotBlank(message = "laylist name must not be empty")
    private String name;
    private String description;
    private Boolean isPublic;
    private MultipartFile imageUrl;

}