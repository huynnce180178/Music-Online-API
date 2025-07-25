package com.ra.base_spring_boot.services.subscription;

import com.ra.base_spring_boot.dto.resp.StatisticSubscriptionResponseDTO;
import com.ra.base_spring_boot.dto.resp.SubscriptionPlanResponseDTO;
import com.ra.base_spring_boot.dto.resp.SubscriptionResponseDTO;
import com.ra.base_spring_boot.model.Subscription;
import com.ra.base_spring_boot.model.User;
import com.ra.base_spring_boot.repository.SubscriptionRepository;

import java.time.LocalDateTime;

public interface SubscriptionService {
    SubscriptionResponseDTO subscribe(Long userId, Long planId);
    void checkAndUpdateExpiredSubscriptions();
    StatisticSubscriptionResponseDTO getStatisticAllPlan(LocalDateTime start, LocalDateTime end);
    SubscriptionResponseDTO getSubscriptionByUser(User user);
}
