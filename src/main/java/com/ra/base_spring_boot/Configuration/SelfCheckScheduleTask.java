package com.ra.base_spring_boot.Configuration;

import com.ra.base_spring_boot.services.subscription.SubscriptionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class SelfCheckScheduleTask {
    @Autowired
    private SubscriptionService subscriptionService;
    @Scheduled(cron = "0 0 0 * * ?")
    public void runSelfCheckSchedule() {
        subscriptionService.checkAndUpdateExpiredSubscriptions();
    }
}
