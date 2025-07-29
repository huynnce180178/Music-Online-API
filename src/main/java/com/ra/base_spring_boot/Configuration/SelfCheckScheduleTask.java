package com.ra.base_spring_boot.Configuration;

import com.ra.base_spring_boot.services.subscription.SubscriptionService;
import com.ra.base_spring_boot.repository.AdvertisementRepository;
import com.ra.base_spring_boot.model.Advertisement;
import com.ra.base_spring_boot.model.constants.ApprovalStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
public class SelfCheckScheduleTask {
    @Autowired
    private SubscriptionService subscriptionService;

    @Autowired
    private AdvertisementRepository advertisementRepository;

    @Scheduled(cron = "0 0 0 * * ?")
    public void runSelfCheckSchedule() {
        subscriptionService.checkAndUpdateExpiredSubscriptions();
        List<Advertisement> expiredAds = advertisementRepository.findByEndDateBefore(LocalDate.now());
        for (Advertisement ad : expiredAds) {
            if (ad.getStatus() != ApprovalStatus.EXPIRED) {
                ad.setStatus(ApprovalStatus.EXPIRED);
            }
        }
        advertisementRepository.saveAll(expiredAds);
    }
}
