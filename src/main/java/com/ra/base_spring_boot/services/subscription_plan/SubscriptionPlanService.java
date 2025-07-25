package com.ra.base_spring_boot.services.subscription_plan;

import com.ra.base_spring_boot.dto.req.SubscriptionPlanRequestDTO;
import com.ra.base_spring_boot.dto.resp.SubscriptionPlanResponseDTO;

import java.util.List;

public interface SubscriptionPlanService {
    List<SubscriptionPlanResponseDTO> getAllSubscriptionPlan();
    SubscriptionPlanResponseDTO getSubscriptionPlanById(Long id);
    SubscriptionPlanResponseDTO saveSubscriptionPlan(SubscriptionPlanRequestDTO subscriptionPlanRequestDTO);
    SubscriptionPlanResponseDTO updateSubscriptionPlan(Long id ,SubscriptionPlanRequestDTO subscriptionPlanRequestDTO);
    void deleteSubscriptionPlan(Long id);
}
