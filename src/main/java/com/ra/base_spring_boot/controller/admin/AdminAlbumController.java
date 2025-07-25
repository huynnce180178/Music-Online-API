package com.ra.base_spring_boot.controller.admin;

import com.ra.base_spring_boot.dto.resp.AlbumResponseDTO;
import com.ra.base_spring_boot.model.Album;
import com.ra.base_spring_boot.model.User;
import com.ra.base_spring_boot.services.album.AlbumService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.context.Context;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Tag(name = "Admin Album Management")
@RestController
@RequestMapping("/api/v1/admin/albums")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminAlbumController {
    private final AlbumService albumService;
    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;

    @Operation(summary = "Lấy danh sách album chờ duyệt",
              description = "Lấy tất cả các album đang trong trạng thái chờ admin phê duyệt")
    @GetMapping("/pending")
    public ResponseEntity<List<AlbumResponseDTO>> getPendingAlbums() {
        List<AlbumResponseDTO> pendingAlbums = albumService.getPendingAlbums();
        return ResponseEntity.ok(pendingAlbums);
    }

    @Operation(summary = "Lấy danh sách album bị từ chối",
              description = "Lấy tất cả các album đã bị admin từ chối")
    @GetMapping("/rejected")
    public ResponseEntity<List<AlbumResponseDTO>> getRejectedAlbums() {
        List<AlbumResponseDTO> rejectedAlbums = albumService.getRejectedAlbums();
        return ResponseEntity.ok(rejectedAlbums);
    }

    @Operation(summary = "Lấy danh sách album bị admin xóa",
              description = "Lấy tất cả các album đã bị admin xóa")
    @GetMapping("/deleted-by-admin")
    public ResponseEntity<List<AlbumResponseDTO>> getDeletedByAdminAlbums() {
        List<AlbumResponseDTO> deletedAlbums = albumService.getDeletedByAdminAlbums();
        return ResponseEntity.ok(deletedAlbums);
    }

    @Operation(summary = "Phê duyệt album",
              description = "Phê duyệt một album đang chờ duyệt và cho phép hiển thị với người dùng")
    @PutMapping("/{albumId}/approve")
    public ResponseEntity<String> approveAlbum(@PathVariable Long albumId) {
        try {
            albumService.approveAlbum(albumId);
            return ResponseEntity.ok("Album has been approved successfully");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Error approving album: " + e.getMessage());
        }
    }

    @Operation(summary = "Từ chối album",
              description = "Từ chối một album và gửi thông báo cho nghệ sĩ kèm lý do tùy chọn")
    @PutMapping("/{albumId}/reject")
    public ResponseEntity<String> rejectAlbum(
            @PathVariable Long albumId,
            @RequestParam(required = false) String reason) {
        try {
            albumService.rejectAlbum(albumId, reason != null ? reason : "Content violation");
            return ResponseEntity.ok("Album has been rejected due to content violation");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Error rejecting album: " + e.getMessage());
        }
    }

    @Operation(summary = "Xóa album",
              description = "Thao tác xóa album của admin và gửi thông báo cho nghệ sĩ")
    @DeleteMapping("/{albumId}/delete")
    public ResponseEntity<String> adminDeleteAlbum(
            @PathVariable Long albumId,
            @RequestParam(required = false) String reason) {
        try {
            Album album = albumService.getAlbumWithArtist(albumId);
            User artist = album.getUsers();
            String artistEmail = artist.getEmail();

            sendAlbumDeletionEmail(
                artist.getFirstName() + " " + artist.getLastName(),
                album.getTitle(),
                reason != null ? reason : "Content policy violation",
                artistEmail
            );

            albumService.deleteAlbumByAdmin(albumId, reason != null ? reason : "Content policy violation");
            return ResponseEntity.ok("Album has been marked as rejected and artist has been notified");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Error processing album deletion: " + e.getMessage());
        }
    }

    @Operation(summary = "Khôi phục album",
              description = "Khôi phục một album đã bị từ chối hoặc xóa về trạng thái chờ duyệt")
    @PutMapping("/{albumId}/restore")
    public ResponseEntity<String> restoreAlbum(@PathVariable Long albumId) {
        try {
            albumService.restoreAlbum(albumId);
            return ResponseEntity.ok("Album has been restored and set to pending status");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Error restoring album: " + e.getMessage());
        }
    }

    private void sendAlbumDeletionEmail(String artistName, String albumName, String reason, String artistEmail)
            throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        Context context = new Context();
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

        context.setVariable("artistName", artistName);
        context.setVariable("albumName", albumName);
        context.setVariable("reason", reason);
        context.setVariable("deletionTime", now.format(formatter));
        context.setVariable("deletionDate", now.format(DateTimeFormatter.ofPattern("EEEE, MMMM dd, yyyy", Locale.ENGLISH)));

        String emailContent = templateEngine.process("email/album-deletion-notification", context);

        helper.setTo(artistEmail);
        helper.setSubject("Your Album Has Been Removed - Music Online");
        helper.setText(emailContent, true);

        mailSender.send(message);
    }

    @Operation(summary = "Lấy tất cả album",
              description = "Lấy danh sách tất cả album trong hệ thống cho admin")
    @GetMapping("/all")
    public ResponseEntity<List<AlbumResponseDTO>> getAllAlbumsForAdmin() {
        List<AlbumResponseDTO> allAlbums = albumService.getAllAlbums();
        return ResponseEntity.ok(allAlbums);
    }
}
