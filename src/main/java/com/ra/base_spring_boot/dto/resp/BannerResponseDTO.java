package com.ra.base_spring_boot.dto.resp;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Builder
@Getter
public class BannerResponseDTO {
    private Long id;
    private String profilePicUrl;
    private String position;
}
