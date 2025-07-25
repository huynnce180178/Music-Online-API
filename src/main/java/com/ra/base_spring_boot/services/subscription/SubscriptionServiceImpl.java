package com.ra.base_spring_boot.services.subscription;

import com.ra.base_spring_boot.dto.req.NotificationRequest;
import com.ra.base_spring_boot.dto.resp.StatisticSubscriptionResponseDTO;
import com.ra.base_spring_boot.dto.resp.SubscriptionResponseDTO;
import com.ra.base_spring_boot.model.Role;
import com.ra.base_spring_boot.model.Subscription;
import com.ra.base_spring_boot.model.SubscriptionPlan;
import com.ra.base_spring_boot.model.User;
import com.ra.base_spring_boot.model.constants.PlanType;
import com.ra.base_spring_boot.model.constants.RoleName;
import com.ra.base_spring_boot.model.constants.SubscriptionStatus;
import com.ra.base_spring_boot.repository.IUserRepository;
import com.ra.base_spring_boot.repository.SubscriptionPlanRepository;
import com.ra.base_spring_boot.repository.SubscriptionRepository;
import com.ra.base_spring_boot.services.EmailVerificationService;
import com.ra.base_spring_boot.services.IRoleService;
import com.ra.base_spring_boot.services.notification.NotificationService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;

@Service
public class SubscriptionServiceImpl implements SubscriptionService {


    @Autowired
    private IUserRepository userRepository;
    @Autowired
    private SubscriptionPlanRepository subscriptionPlanRepository;
    @Autowired
    private SubscriptionRepository subscriptionRepository;
    @Autowired
    private IRoleService roleService;
    @Autowired
    private EmailVerificationService emailVerificationService;
    @Autowired
    private NotificationService notificationService;

    private Long daysRemaining(Subscription subscription) {
        long days = Duration.between(LocalDateTime.now(), subscription.getEndTime()).toDays();
        return Math.max(days, 0L);
    }
    private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");


    @Override
    public SubscriptionResponseDTO subscribe(Long userId, Long planId) {
        User user = userRepository.getById(userId);
        SubscriptionPlan subscriptionPlan = subscriptionPlanRepository.getById(planId);
        Set<Role> roles = user.getRoles();

        Subscription subscription = new Subscription();
        subscription.setUser(user);
        if (subscriptionPlan.getPlanType() == PlanType.ARTIST) {
            subscription.setPlan(com.ra.base_spring_boot.model.constants.SubscriptionPlan.ARTIST);
            roles.add(roleService.findByRoleName(RoleName.ROLE_ARTIST));
            user.setRoles(roles);
        } else if (subscriptionPlan.getPlanType() == PlanType.PREMIUM) {
            subscription.setPlan(com.ra.base_spring_boot.model.constants.SubscriptionPlan.PREMIUM);
            roles.add(roleService.findByRoleName(RoleName.ROLE_PREMIUM));
            user.setRoles(roles);
        }
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setStartTime(LocalDateTime.now());
        subscription.setEndTime(LocalDateTime.now().plusDays(subscriptionPlan.getDurationDay()));
        subscription.setCreatedAt(LocalDateTime.now());
        subscription.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
        subscriptionRepository.save(subscription);
        return SubscriptionResponseDTO.builder()
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .plan(subscription.getPlan())
                .startTime(subscription.getStartTime().format(formatter))
                .endTime(subscription.getEndTime().format(formatter))
                .daysRemaining(daysRemaining(subscription))
                .status(subscription.getStatus())
                .build();
    }

    @Transactional(rollbackOn = {Exception.class, Throwable.class})
    @Override
    public void checkAndUpdateExpiredSubscriptions() {
        List<Subscription> subscriptions = subscriptionRepository.findByEndTimeBeforeAndStatus(LocalDateTime.now(), SubscriptionStatus.ACTIVE);
        for (Subscription subscription : subscriptions) {
            User user = subscription.getUser();
            if (subscription.getPlan() == com.ra.base_spring_boot.model.constants.SubscriptionPlan.PREMIUM) {
                Role premiumRole = roleService.findByRoleName(RoleName.ROLE_PREMIUM);
                user.getRoles().remove(premiumRole);
                emailVerificationService.sendMailExpire(subscription);
            } else if (subscription.getPlan() == com.ra.base_spring_boot.model.constants.SubscriptionPlan.ARTIST) {
                Role artistRole = roleService.findByRoleName(RoleName.ROLE_ARTIST);
                user.getRoles().remove(artistRole);
                emailVerificationService.sendMailExpire(subscription);
            }
            subscription.setStatus(SubscriptionStatus.EXPIRED);
            subscription.setUpdatedAt(LocalDateTime.now());
            userRepository.save(user);
            subscriptionRepository.save(subscription);
            String message = "Your subscription has expired.";
            notificationService.createNotification(
                    new NotificationRequest(
                            user.getId(),
                            message,
                            "subscription_expired"
                    )
            );
        }

    }

    @Override
    public StatisticSubscriptionResponseDTO getStatisticAllPlan(LocalDateTime start, LocalDateTime end) {
        if (start == null && end == null) {
            return StatisticSubscriptionResponseDTO.builder()
                    .totalSubscription(subscriptionRepository.count())
                    .totalPlanPremium(subscriptionRepository.countByPlan(com.ra.base_spring_boot.model.constants.SubscriptionPlan.PREMIUM))
                    .totalPlanArtist(subscriptionRepository.countByPlan(com.ra.base_spring_boot.model.constants.SubscriptionPlan.ARTIST))
                    .build();
        } else if (start.isAfter(end)) {
            throw new IllegalArgumentException("start time is after end time");
        }
        return StatisticSubscriptionResponseDTO.builder()
                .totalSubscription(subscriptionRepository.countSubscriptionsByDate(start, end))
                .totalPlanPremium(subscriptionRepository.countSubscriptionsByDateAndPlan(start, end, com.ra.base_spring_boot.model.constants.SubscriptionPlan.PREMIUM))
                .totalPlanArtist(subscriptionRepository.countSubscriptionsByDateAndPlan(start, end, com.ra.base_spring_boot.model.constants.SubscriptionPlan.ARTIST))
                .build();


    }
    @Override
    public SubscriptionResponseDTO getSubscriptionByUser(User user) {
        List<Subscription> subscriptions = subscriptionRepository.findByUser(user);
        if(subscriptions == null || subscriptions.isEmpty()){
            if (subscriptions == null || subscriptions.isEmpty()) {
                return SubscriptionResponseDTO.builder()
                        .firstName(user.getFirstName())
                        .lastName(user.getLastName())
                        .plan(com.ra.base_spring_boot.model.constants.SubscriptionPlan.FREE)
                        .startTime("FOREVER")
                        .endTime("FOREVER")
                        .daysRemaining(-1L)
                        .status(SubscriptionStatus.ACTIVE)
                        .build();
            }
        }
        Subscription subscription = subscriptions.get(0);
        return SubscriptionResponseDTO.builder()
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .plan(subscription.getPlan())
                .startTime(subscription.getStartTime().format(formatter))
                .endTime(subscription.getEndTime().format(formatter))
                .daysRemaining(daysRemaining(subscription))
                .status(subscription.getStatus())
                .build();
    }
}
