package com.ra.base_spring_boot.dto.resp;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
@Getter
@Setter
public class NotificationResponse {
    private Long id;
    private Long userId;
    private String message;
    private String type;
    private LocalDateTime createdAt;
    private boolean isRead;
}

