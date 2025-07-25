package com.ra.base_spring_boot.dto.resp;

import com.ra.base_spring_boot.model.Subscription;
import com.ra.base_spring_boot.model.constants.SubscriptionPlan;
import com.ra.base_spring_boot.model.constants.SubscriptionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Builder
@Getter
@Setter
@AllArgsConstructor
public class SubscriptionResponseDTO {
    private String firstName;
    private String lastName;
    private SubscriptionPlan plan;
    private String startTime;
    private String endTime;
    private Long daysRemaining;
    private SubscriptionStatus status;
}
