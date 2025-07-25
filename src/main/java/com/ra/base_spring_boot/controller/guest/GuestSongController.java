package com.ra.base_spring_boot.controller.guest;

import com.ra.base_spring_boot.dto.resp.SongDetailDTO;
import com.ra.base_spring_boot.services.song.SongService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name="Guest Song Controller")
@RequestMapping("/api/v1/guest")
@Validated
@RestController
public class GuestSongController {
    private final SongService songService;

    public GuestSongController(SongService songService) {
        this.songService = songService;
    }

    @Operation(summary = "Xem chi tiết bài hát",
            description = "Lấy thông tin chi tiết của một bài hát bao gồm cả thông tin nghệ sĩ và bình luận. Hoạt động cho cả guest và user đã đăng nhập")
    @GetMapping("/{songId}/details")
    public ResponseEntity<SongDetailDTO> getSongDetails(@PathVariable Long songId, Authentication authentication) {
        try {
            SongDetailDTO detail;

            if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal())) {
                detail = songService.getSongDetailWithAuth(songId, authentication);
            } else {
                detail = songService.getSongDetailForGuest(songId);
            }

            return ResponseEntity.ok(detail);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(null);
        }
    }
}
