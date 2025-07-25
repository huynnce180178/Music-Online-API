package com.ra.base_spring_boot.model;

import com.ra.base_spring_boot.model.base.BaseObject;
import com.ra.base_spring_boot.model.constants.SubscriptionPlan;
import com.ra.base_spring_boot.model.constants.SubscriptionStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@Table(name = "subscriptions")
public class Subscription extends BaseObject {
    @Enumerated(EnumType.STRING)
    private SubscriptionPlan plan;
    @Column(name="start_time")
    private LocalDateTime startTime;
    @Column(name = "end_time")
    private LocalDateTime endTime;
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private SubscriptionStatus status;
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @ManyToOne
    @JoinColumn(name="user_id", referencedColumnName = "id")
    private User user;





}
