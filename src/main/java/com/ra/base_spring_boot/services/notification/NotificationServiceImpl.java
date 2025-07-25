package com.ra.base_spring_boot.services.notification;

import com.ra.base_spring_boot.controller.NotificationWebSocketController;
import com.ra.base_spring_boot.dto.req.NotificationRequest;
import com.ra.base_spring_boot.dto.resp.NotificationResponse;
import com.ra.base_spring_boot.model.Notification;
import com.ra.base_spring_boot.model.User;
import com.ra.base_spring_boot.repository.IUserRepository;
import com.ra.base_spring_boot.repository.NotificationRepository;
import com.ra.base_spring_boot.services.user.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class NotificationServiceImpl implements NotificationService {
    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private IUserRepository userRepository;

    @Autowired
    private NotificationWebSocketController notificationWebSocketController;

    private NotificationResponse toResponse(Notification notification) {
        NotificationResponse response = new NotificationResponse();
        response.setId(notification.getId());
        response.setUserId(notification.getUser() != null ? notification.getUser().getId() : null);
        response.setMessage(notification.getMessage());
        response.setType(notification.getType());
        response.setCreatedAt(notification.getCreatedAt());
        response.setRead(notification.isRead());
        return response;
    }

    private User getUserById(Long userId) {
        return userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
    }

    @Override
    public NotificationResponse createNotification(NotificationRequest request) {
        Notification notification = new Notification();
        notification.setUser(getUserById(request.getUserId()));
        notification.setMessage(request.getMessage());
        notification.setType(request.getType());
        notification.setCreatedAt(LocalDateTime.now());
        notification.setRead(false);
        Notification saved = notificationRepository.save(notification);
        NotificationResponse response = toResponse(saved);
        // Gửi notification qua WebSocket
        notificationWebSocketController.sendNotificationToUser(response.getUserId(), response);
        return response;
    }

    @Override
    public List<NotificationResponse> getNotificationsByUser(Authentication authentication) {
        Long userId = getUserIdFromAuthentication(authentication);
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public Optional<NotificationResponse> getNotificationById(Long id, Authentication authentication) {
        Long userId = getUserIdFromAuthentication(authentication);
        return notificationRepository.findById(id)
                .filter(n -> n.getUser() != null && n.getUser().getId().equals(userId))
                .map(this::toResponse);
    }

    @Override
    public NotificationResponse markAsRead(Long id, Authentication authentication) {
        Optional<Notification> optional = notificationRepository.findById(id);
        Long userId = getUserIdFromAuthentication(authentication);
        if (optional.isPresent() && optional.get().getUser() != null && optional.get().getUser().getId().equals(userId)) {
            Notification notification = optional.get();
            notification.setRead(true);
            Notification saved = notificationRepository.save(notification);
            return toResponse(saved);
        }
        return null;
    }

    @Override
    public void deleteNotification(Long id, Authentication authentication) {
        Optional<Notification> optional = notificationRepository.findById(id);
        Long userId = getUserIdFromAuthentication(authentication);
        if (optional.isPresent() && optional.get().getUser() != null && optional.get().getUser().getId().equals(userId)) {
            notificationRepository.deleteById(id);
        }
    }

    @Override
    public void notifySongApproved(Long userId, String songTitle, List<Long> followerIds) {
        String messageForFollowers = "The new song '" + songTitle + "' by the artist you follow has been approved.";
        String messageForArtist = "Your song '" + songTitle + "' has been approved.";
        // Notify followers
        for (Long followerId : followerIds) {
            Notification notification = new Notification();
            notification.setUser(getUserById(followerId));
            notification.setMessage(messageForFollowers);
            notification.setType("song");
            notification.setCreatedAt(LocalDateTime.now());
            notification.setRead(false);
            Notification saved = notificationRepository.save(notification);
            NotificationResponse response = toResponse(saved);
            notificationWebSocketController.sendNotificationToUser(response.getUserId(), response);
        }
        // Notify artist
        Notification artistNotification = new Notification();
        artistNotification.setUser(getUserById(userId));
        artistNotification.setMessage(messageForArtist);
        artistNotification.setType("song");
        artistNotification.setCreatedAt(LocalDateTime.now());
        artistNotification.setRead(false);
        Notification savedArtist = notificationRepository.save(artistNotification);
        NotificationResponse responseArtist = toResponse(savedArtist);
        notificationWebSocketController.sendNotificationToUser(responseArtist.getUserId(), responseArtist);
    }

    @Override
    public void notifyAlbumApproved(Long userId, String albumTitle, List<Long> followerIds) {
        String messageForFollowers = "The new album '" + albumTitle + "' of the artist you follow has been approved.";
        String messageForArtist = "Your album '" + albumTitle + "' has been approved.";
        for (Long followerId : followerIds) {
            Notification notification = new Notification();
            notification.setUser(userService.getArtistById(followerId));
            notification.setMessage("message");
            notification.setType("album");
            notification.setCreatedAt(LocalDateTime.now());
            notification.setRead(false);
            Notification saved = notificationRepository.save(notification);
            NotificationResponse response = toResponse(saved);
            notificationWebSocketController.sendNotificationToUser(response.getUserId(), response);
        }
    }

    // Thông báo cho chính artist khi bài hát của họ được duyệt
    @Override
    public void notifyArtistSongApproved(Long userId, String songTitle) {
        String message = "Your song '" + songTitle + "' has been approved.";
        Notification notification = new Notification();
        notification.setUser(userService.getArtistById(userId));
        notification.setMessage(message);
        notification.setType("song");
        notification.setCreatedAt(LocalDateTime.now());
        notification.setRead(false);
        Notification saved = notificationRepository.save(notification);
        NotificationResponse response = toResponse(saved);
        notificationWebSocketController.sendNotificationToUser(response.getUserId(), response);
    }

    // Thông báo cho chính artist khi album của họ được duyệt
    @Override
    public void notifyArtistAlbumApproved(Long userId, String albumTitle) {
        String message = "Your album '" + albumTitle + "' has been approved.";
        Notification notification = new Notification();
        notification.setUser(userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Artist not found with id: " + userId)));
        notification.setMessage(message);
        notification.setType("album");
        notification.setCreatedAt(LocalDateTime.now());
        notification.setRead(false);
        Notification saved = notificationRepository.save(notification);
        NotificationResponse response = toResponse(saved);
        notificationWebSocketController.sendNotificationToUser(response.getUserId(), response);
    }

    @Override
    public void notifyAlbumRejected(Long userId, String albumTitle, String reason, List<Long> followerIds) {
        String message = "Your album '" + albumTitle + "' was rejected. Reason: " + reason;
        // Gửi cho artist
        Notification notification = new Notification();
        notification.setUser(userService.getArtistById(userId));
        notification.setMessage(message);
        notification.setType("album_rejected");
        notification.setCreatedAt(LocalDateTime.now());
        notification.setRead(false);
        Notification saved = notificationRepository.save(notification);
        NotificationResponse response = toResponse(saved);
        notificationWebSocketController.sendNotificationToUser(response.getUserId(), response);
    }

    @Override
    @Transactional
    public int clearAllNotifications(Authentication authentication) {
        Long userId = getUserIdFromAuthentication(authentication);
        return notificationRepository.deleteByUserId(userId);
    }

    @Override
    public long countUnreadNotifications(Authentication authentication) {
        Long userId = getUserIdFromAuthentication(authentication);
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }


    @Override
    public Long getUserIdFromAuthentication(Authentication authentication) {
        String email = authentication.getName();
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isPresent()) {
            return userOpt.get().getId();
        } else {
            throw new RuntimeException("User not found with email: " + email);
        }

    }
}
