package com.ra.base_spring_boot.controller.admin;

import com.ra.base_spring_boot.dto.resp.SongResponseDTO;
import com.ra.base_spring_boot.model.Song;
import com.ra.base_spring_boot.model.User;
import com.ra.base_spring_boot.services.song.SongService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Tag(name = "Admin Song Management")
@RestController
@RequestMapping("/api/v1/admin/songs")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminSongController {
    private final SongService songService;

    @Operation(summary = "Lấy danh sách bài hát chờ duyệt",
              description = "Lấy tất cả các bài hát đang trong trạng thái chờ admin phê duyệt")
    @GetMapping("/pending")
    public ResponseEntity<List<SongResponseDTO>> getPendingSongs() {
        List<SongResponseDTO> pendingSongs = songService.getPendingSongs();
        return ResponseEntity.ok(pendingSongs);
    }

    @Operation(summary = "Lấy danh sách bài hát bị từ chối",
              description = "Lấy tất cả các bài hát đã bị admin từ chối")
    @GetMapping("/rejected")
    public ResponseEntity<List<SongResponseDTO>> getRejectedSongs() {
        List<SongResponseDTO> rejectedSongs = songService.getRejectedSongs();
        return ResponseEntity.ok(rejectedSongs);
    }

    @Operation(summary = "Lấy danh sách bài hát bị admin xóa",
              description = "Lấy tất cả các bài hát đã bị admin xóa")
    @GetMapping("/deleted-by-admin")
    public ResponseEntity<List<SongResponseDTO>> getDeletedByAdminSongs() {
        List<SongResponseDTO> deletedByAdminSongs = songService.getDeletedByAdminSongs();
        return ResponseEntity.ok(deletedByAdminSongs);
    }

    @Operation(summary = "Lấy danh sách bài hát bị nghệ sĩ xóa",
              description = "Lấy tất cả các bài hát đã bị nghệ sĩ tự xóa")
    @GetMapping("/deleted-by-artist")
    public ResponseEntity<List<SongResponseDTO>> getDeletedByArtistSongs() {
        List<SongResponseDTO> deletedSongs = songService.getDeletedByArtistSongs();
        return ResponseEntity.ok(deletedSongs);
    }

    @Operation(summary = "Phê duyệt bài hát",
              description = "Phê duyệt một bài hát đang chờ duyệt và cho phép hiển thị với người dùng")
    @PutMapping("/{songId}/approve")
    public ResponseEntity<String> approveSong(@PathVariable Long songId) {
        try {
            songService.approveSong(songId);
            return ResponseEntity.ok("Song has been approved successfully");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Error approving song: " + e.getMessage());
        }
    }

    @Operation(summary = "Từ chối bài hát",
              description = "Từ chối một bài hát và gửi thông báo cho nghệ sĩ kèm lý do tùy chọn")
    @PutMapping("/{songId}/reject")
    public ResponseEntity<String> rejectSong(
            @PathVariable Long songId,
            @RequestParam(required = false) String reason) {
        try {
            songService.rejectSong(songId, reason != null ? reason : "Content violation");
            return ResponseEntity.ok("Song has been rejected due to content violation");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Error rejecting song: " + e.getMessage());
        }
    }

    @Operation(summary = "Khôi phục bài hát",
              description = "Khôi phục một bài hát đã bị từ chối hoặc xóa về trạng thái chờ duyệt")
    @PutMapping("/{songId}/restore")
    public ResponseEntity<String> restoreSong(@PathVariable Long songId) {
        try {
            songService.restoreSong(songId);
            return ResponseEntity.ok("Song has been restored and set to pending status");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Error restoring song: " + e.getMessage());
        }
    }

    @Operation(summary = "Lấy tất cả bài hát",
              description = "Lấy danh sách tất cả bài hát trong hệ thống cho admin")
    @GetMapping("/all")
    public ResponseEntity<List<SongResponseDTO>> getAllSongsForAdmin() {
        List<SongResponseDTO> allSongs = songService.getAllSongs();
        return ResponseEntity.ok(allSongs);
    }

    @Operation(summary = "Xóa bài hát",
              description = "Thao tác xóa bài hát của admin và gửi thông báo cho nghệ sĩ")
    @DeleteMapping("/{songId}/delete")
    public ResponseEntity<String> adminDeleteSong(
            @PathVariable Long songId,
            @RequestParam(required = false) String reason) {
        try {
            songService.deleteSongByAdmin(songId, reason != null ? reason : "Inappropriate Content");
            return ResponseEntity.ok("Song has been deleted by admin and artist has been notified");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Error deleting song: " + e.getMessage());
        }
    }
}
