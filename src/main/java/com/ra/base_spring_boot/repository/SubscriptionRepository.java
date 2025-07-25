package com.ra.base_spring_boot.repository;

import com.ra.base_spring_boot.model.Subscription;
import com.ra.base_spring_boot.model.User;
import com.ra.base_spring_boot.model.constants.SubscriptionPlan;
import com.ra.base_spring_boot.model.constants.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    List<Subscription> findByEndTimeBeforeAndStatus(LocalDateTime time, SubscriptionStatus status);
    Long countByPlan(SubscriptionPlan plan);
    @Query("""
    SELECT COUNT(s)
    FROM Subscription s
    WHERE s.createdAt>:start
      AND s.createdAt<:end
      AND s.plan = :plan
""")
    Long countSubscriptionsByDateAndPlan(LocalDateTime start, LocalDateTime end, SubscriptionPlan plan);
    @Query("""
    SELECT COUNT(s)
    FROM Subscription s
    WHERE s.createdAt>:start
      AND s.createdAt<:end
""")
    Long countSubscriptionsByDate(LocalDateTime start, LocalDateTime end);

    List<Subscription> findByUser(User user);
}
