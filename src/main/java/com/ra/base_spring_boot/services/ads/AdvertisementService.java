package com.ra.base_spring_boot.services.ads;

import com.ra.base_spring_boot.dto.req.AdvertisementRequest;
import com.ra.base_spring_boot.dto.resp.AdvertisementResponse;

import java.util.List;

public interface AdvertisementService {
    AdvertisementResponse createAdvertisement(AdvertisementRequest request, String creatorEmail);
    List<AdvertisementResponse> getAllAdvertisements();
    AdvertisementResponse approveAdvertisement(Long adId);
    AdvertisementResponse rejectAdvertisement(Long adId, String reason);

    List<AdvertisementResponse> getPendingAdvertisements();
    List<AdvertisementResponse> getRejectedAdvertisements();
    List<AdvertisementResponse> getApprovedBannerList();
    AdvertisementResponse getRandomApprovedAudioAd();
    int deleteExpiredAdvertisements();
    AdvertisementResponse renewAdvertisement(Long adId, String startDateStr, String endDateStr, String artistEmail);
    List<AdvertisementResponse> getArtistApprovedAdvertisements(String artistEmail);
    List<AdvertisementResponse> getAllApprovedAdvertisements();
    List<AdvertisementResponse> getMyApprovedAdvertisements();
    void deleteMyAdvertisement(Long adId);
}
