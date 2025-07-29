package com.ra.base_spring_boot.controller;

import com.ra.base_spring_boot.dto.resp.NotificationResponse;
import com.ra.base_spring_boot.dto.resp.UserBlockNotificationDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class NotificationWebSocketController {
    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    public void sendNotificationToUser(Long userId, NotificationResponse notification) {
        messagingTemplate.convertAndSend("/topic/notifications/" + userId, notification);
    }

    public void sendUserBlockNotification(Long userId, UserBlockNotificationDTO blockNotification) {
        messagingTemplate.convertAndSend("/topic/user-status/" + userId, blockNotification);
    }
}
