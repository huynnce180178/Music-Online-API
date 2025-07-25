package com.ra.base_spring_boot.dto.resp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
@Builder
public class StatisticSubscriptionResponseDTO {
    private Long totalSubscription;
    private Long totalPlanPremium;
    private Long totalPlanArtist;
}
