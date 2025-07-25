package com.ra.base_spring_boot.services.notification;


import com.ra.base_spring_boot.dto.req.NotificationRequest;
import com.ra.base_spring_boot.dto.resp.NotificationResponse;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Optional;

public interface NotificationService {
    NotificationResponse createNotification(NotificationRequest request);
    List<NotificationResponse> getNotificationsByUser(Authentication authentication);
    Optional<NotificationResponse> getNotificationById(Long id, Authentication authentication);
    NotificationResponse markAsRead(Long id, Authentication authentication);
    void deleteNotification(Long id, Authentication authentication);
    void notifySongApproved(Long userId, String songTitle, List<Long> followerIds);
    void notifyAlbumApproved(Long userId, String albumTitle, List<Long> followerIds);
    void notifyArtistSongApproved(Long userId, String songTitle);
    void notifyArtistAlbumApproved(Long userId, String albumTitle);
    void notifyAlbumRejected(Long userId, String albumTitle, String reason, List<Long> followerIds);
    int clearAllNotifications(Authentication authentication);
    long countUnreadNotifications(Authentication authentication);
    Long getUserIdFromAuthentication(Authentication authentication);
}
