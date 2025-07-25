package com.ra.base_spring_boot.dto.req;

import com.ra.base_spring_boot.model.constants.PlanType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
@Setter
@AllArgsConstructor
public class SubscriptionPlanRequestDTO {
    private String planName;
    private double price;
    private PlanType planType;
    private int durationDay;
    private String description;

}
