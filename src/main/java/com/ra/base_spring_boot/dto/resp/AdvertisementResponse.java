package com.ra.base_spring_boot.dto.resp;

import lombok.Data;

import java.time.LocalDate;

@Data
public class AdvertisementResponse {
    private Long id;
    private String adType;
    private String status;
    private String mediaUrl;
    private String redirectUrl;
    private LocalDate startDate;
    private LocalDate endDate;
    private Long createdById;
    private String createdByEmail;
    private String createdByName;
    private String rejectReason;
    private String startDateStr;
    private String endDateStr;
}
