package com.ra.base_spring_boot.dto.resp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserBlockNotificationDTO {
    private Long userId;
    private String username;
    private String message;
    private String type;
    private LocalDateTime timestamp;
    private boolean requireLogout;
}
