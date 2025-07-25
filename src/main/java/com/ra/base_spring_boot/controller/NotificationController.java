package com.ra.base_spring_boot.controller;

import com.ra.base_spring_boot.dto.ResponseWrapper;
import com.ra.base_spring_boot.dto.resp.NotificationResponse;
import com.ra.base_spring_boot.services.notification.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/user/notifications")
public class NotificationController {
    @Autowired
    private NotificationService notificationService;

    @Operation(summary = "Lấy danh sách thông báo của chính mình", description = "Chỉ trả về thông báo của người dùng đang đăng nhập")
    @GetMapping("")
    public ResponseEntity<ResponseWrapper<Object>> getMyNotifications(Authentication authentication) {
        List<NotificationResponse> notifications = notificationService.getNotificationsByUser(authentication);
        return ResponseEntity.ok(
            ResponseWrapper.builder()
                .data(notifications)
                .code(HttpStatus.OK.value())
                .status(HttpStatus.OK)
                .build()
        );
    }

    @Operation(summary = "Đánh dấu đã đọc", description = "Chỉ cho phép người dùng đánh dấu thông báo của chính mình")
    @PutMapping("/{id}/read")
    public ResponseEntity<ResponseWrapper<Object>> markAsRead(@PathVariable Long id, Authentication authentication) {
        NotificationResponse response = notificationService.markAsRead(id, authentication);
        if (response != null) {
            return ResponseEntity.ok(
                ResponseWrapper.builder()
                    .data(response)
                    .code(HttpStatus.OK.value())
                    .status(HttpStatus.OK)
                    .build()
            );
        }
        return ResponseEntity.ok(
            ResponseWrapper.builder()
                .data("You do not have permission to mark this notification as read.")
                .code(HttpStatus.FORBIDDEN.value())
                .status(HttpStatus.FORBIDDEN)
                .build()
        );
    }

    @Operation(summary = "Xóa thông báo", description = "Chỉ cho phép người dùng xóa thông báo của chính mình")
    @DeleteMapping("/{id}")
    public ResponseEntity<ResponseWrapper<Object>> delete(@PathVariable Long id, Authentication authentication) {
        Optional<NotificationResponse> notificationOpt = notificationService.getNotificationById(id, authentication);
        if (notificationOpt.isPresent()) {
            notificationService.deleteNotification(id, authentication);
            return ResponseEntity.ok(
                ResponseWrapper.builder()
                    .data("Deleted notification with id: " + id)
                    .code(HttpStatus.OK.value())
                    .status(HttpStatus.OK)
                    .build()
            );
        }
        return ResponseEntity.ok(
            ResponseWrapper.builder()
                .data("You do not have permission to delete this notification.")
                .code(HttpStatus.FORBIDDEN.value())
                .status(HttpStatus.FORBIDDEN)
                .build()
        );
    }

    @Operation(summary = "Xóa tất cả thông báo", description = "Chỉ cho phép người dùng xóa toàn bộ thông báo của chính mình")
    @DeleteMapping("/clear")
    public ResponseEntity<ResponseWrapper<Object>> clearAll(Authentication authentication) {
        int deletedCount = notificationService.clearAllNotifications(authentication);
        return ResponseEntity.ok(
            ResponseWrapper.builder()
                .data("Deleted " + deletedCount + " notification!")
                .code(HttpStatus.OK.value())
                .status(HttpStatus.OK)
                .build()
        );
    }

    @Operation(summary = "Đếm thông báo chưa đọc", description = "Chỉ cho phép người dùng đếm thông báo chưa đọc của chính mình")
    @GetMapping("/count-unread")
    public ResponseEntity<ResponseWrapper<Object>> countUnread(Authentication authentication) {
        long count = notificationService.countUnreadNotifications(authentication);
        return ResponseEntity.ok(
            ResponseWrapper.builder()
                .data(count)
                .code(HttpStatus.OK.value())
                .status(HttpStatus.OK)
                .build()
        );
    }
}
