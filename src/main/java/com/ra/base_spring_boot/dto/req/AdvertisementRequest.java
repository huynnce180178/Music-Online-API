package com.ra.base_spring_boot.dto.req;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class AdvertisementRequest {
    private String adType;
    private MultipartFile mediaFile;
    private String redirectUrl;
}
