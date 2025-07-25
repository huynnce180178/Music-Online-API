package com.ra.base_spring_boot.dto.resp;

import com.ra.base_spring_boot.model.constants.PlanType;
import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
@Setter
@AllArgsConstructor
public class SubscriptionPlanResponseDTO {
    private Long id;
    private String planName;
    private double price;
    private PlanType planType;
    private int durationDay;
    private String description;
}
