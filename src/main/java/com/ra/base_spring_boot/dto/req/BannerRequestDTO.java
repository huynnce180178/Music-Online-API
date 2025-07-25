package com.ra.base_spring_boot.dto.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import org.springframework.web.multipart.MultipartFile;


@Builder
@Getter
public class BannerRequestDTO {
    @NotNull(message = "File must not be null")
    private MultipartFile file;
    @NotBlank(message = "Position must not be blank")
    private String position;
}
