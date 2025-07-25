package com.ra.base_spring_boot.dto.resp;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
public class TopGenreResponseDTO {
    private Long genreId;
    private String genreName;
    private String genreImage;
    private BigDecimal totalViews;
}
