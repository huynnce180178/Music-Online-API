package com.ra.base_spring_boot.dto.req;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

@Data
public class AdvertisementRequest {
    private String adType;
    private LocalDate startDate;
    private LocalDate endDate;
    private MultipartFile mediaFile;
    private String redirectUrl;
}
