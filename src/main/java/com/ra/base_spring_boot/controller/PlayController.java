package com.ra.base_spring_boot.controller;

import com.ra.base_spring_boot.dto.ResponseWrapper;
import com.ra.base_spring_boot.dto.resp.SongHistoryResponseDTO;
import com.ra.base_spring_boot.dto.resp.SongResponseDTO;
import com.ra.base_spring_boot.security.principle.MyUserDetails;
import com.ra.base_spring_boot.services.play.PlayService;
import com.ra.base_spring_boot.services.songhistory.SongHistoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Play Music")
@RestController
@RequestMapping("/api/v1/play")
@RequiredArgsConstructor
public class PlayController {
    private final PlayService playService;
    private final SongHistoryService songHistoryService;

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof MyUserDetails) {
            MyUserDetails userDetails = (MyUserDetails) authentication.getPrincipal();
            return userDetails.getUser().getId();
        }
        return null;
    }

    @Operation(summary = "Phát bài hát theo ID", description = "Phát một bài hát cụ thể theo songId")
    @GetMapping("/song/{songId}")
    public ResponseEntity<ResponseWrapper<?>> playSong(@PathVariable Long songId) {
        SongResponseDTO song = playService.playSong(songId);

        Long userId = getCurrentUserId();
        if (userId != null) {
            songHistoryService.addToHistory(songId, userId);
        }

        return ResponseEntity.ok(ResponseWrapper.builder()
                .data(song)
                .code(HttpStatus.OK.value())
                .status(HttpStatus.OK)
                .build());
    }

    @Operation(summary = "Phát bài kế tiếp", description = "Phát bài tiếp theo dựa trên songId hiện tại")
    @GetMapping("/song/next/{songId}")
    public ResponseEntity<ResponseWrapper<?>> playNextSong(@PathVariable Long songId) {
        SongResponseDTO song = playService.playNextSong(songId);
        return ResponseEntity.ok(ResponseWrapper.builder()
                .data(song)
                .code(HttpStatus.OK.value())
                .status(HttpStatus.OK)
                .build());
    }

    @Operation(summary = "Phát bài trước đó", description = "Phát bài trước đó dựa trên songId hiện tại")
    @GetMapping("/song/prev/{songId}")
    public ResponseEntity<ResponseWrapper<?>> playPrevSong(@PathVariable Long songId) {
        SongResponseDTO song = playService.playPrevSong(songId);
        return ResponseEntity.ok(ResponseWrapper.builder()
                .data(song)
                .code(HttpStatus.OK.value())
                .status(HttpStatus.OK)
                .build());
    }

    @Operation(summary = "Phát bài từ lịch sử", description = "Phát bài hát đã nghe từ lịch sử phát nhạc dựa trên historyId")
    @GetMapping("/history/{historyId}")
    public ResponseEntity<ResponseWrapper<?>> playSongFromHistory(@PathVariable Long historyId) {
        SongHistoryResponseDTO dto = playService.playSongFromHistory(historyId);
        // Changed from NO_CONTENT to OK, as it's returning data
        return ResponseEntity.ok(
                ResponseWrapper.builder()
                        .data(dto)
                        .code(HttpStatus.OK.value())
                        .status(HttpStatus.OK)
                        .build()
        );
    }

    @Operation(summary = "Phát bài kế tiếp trong lịch sử", description = "Phát bài tiếp theo từ lịch sử phát nhạc")
    @GetMapping("/history/next/{historyId}")
    public ResponseEntity<ResponseWrapper<?>> playNextSongOfHistory(@PathVariable Long historyId) {
        SongHistoryResponseDTO dto = playService.playNextSongOfHistory(historyId);
        // Changed from NO_CONTENT to OK, as it's returning data
        return ResponseEntity.ok(
                ResponseWrapper.builder()
                        .data(dto)
                        .code(HttpStatus.OK.value())
                        .status(HttpStatus.OK)
                        .build()
        );
    }

    @Operation(summary = "Phát bài trước đó trong lịch sử", description = "Phát bài trước đó từ lịch sử phát nhạc")
    @GetMapping("/history/prev/{historyId}")
    public ResponseEntity<ResponseWrapper<?>> playPrevSongOfHistory(@PathVariable Long historyId) {
        SongHistoryResponseDTO dto = playService.playPrevSongOfHistory(historyId);
        // Changed from NO_CONTENT to OK, as it's returning data
        return ResponseEntity.ok(
                ResponseWrapper.builder()
                        .data(dto)
                        .code(HttpStatus.OK.value())
                        .status(HttpStatus.OK)
                        .build()
        );
    }

    @Operation(summary = "Phát bài từ playlist", description = "Phát một bài hát trong playlist")
    @GetMapping("/playlist/{playlistId}/song/{songId}")
    public ResponseEntity<ResponseWrapper<?>> playSongFromPlaylist(@PathVariable Long playlistId, @PathVariable Long songId) {
        SongResponseDTO song = playService.playSongFromPlaylist(songId, playlistId);
        // Changed from NO_CONTENT to OK, as it's returning data
        return ResponseEntity.ok(
                ResponseWrapper.builder()
                        .data(song)
                        .code(HttpStatus.OK.value())
                        .status(HttpStatus.OK)
                        .build()
        );
    }

    @Operation(summary = "Phát bài kế tiếp trong playlist", description = "Phát bài tiếp theo trong playlist")
    @GetMapping("/playlist/{playlistId}/next/{songId}")
    public ResponseEntity<ResponseWrapper<?>> playNextFromPlaylist(@PathVariable Long playlistId, @PathVariable Long songId) {
        SongResponseDTO song = playService.playNextSongFromPlaylist(songId, playlistId);
        return ResponseEntity.ok(ResponseWrapper.builder()
                .data(song)
                .code(HttpStatus.OK.value())
                .status(HttpStatus.OK)
                .build());
    }

    @Operation(summary = "Phát bài trước đó trong playlist", description = "Phát bài trước đó trong playlist")
    @GetMapping("/playlist/{playlistId}/prev/{songId}")
    public ResponseEntity<ResponseWrapper<?>> playPrevFromPlaylist(@PathVariable Long playlistId, @PathVariable Long songId) {
        SongResponseDTO song = playService.playPrevSongFromPlaylist(songId, playlistId);
        return ResponseEntity.ok(ResponseWrapper.builder()
                .data(song)
                .code(HttpStatus.OK.value())
                .status(HttpStatus.OK)
                .build());
    }

    @Operation(summary = "Phát bài từ danh sách yêu thích", description = "Phát bài hát từ danh sách yêu thích của người dùng")
    @GetMapping("/wishlist/{songId}")
    public ResponseEntity<ResponseWrapper<?>> playSongFromWishlist(@PathVariable Long songId) {
        SongResponseDTO song = playService.playSongFromWishlist(songId);
        return ResponseEntity.ok(ResponseWrapper.builder()
                .data(song)
                .code(HttpStatus.OK.value())
                .status(HttpStatus.OK)
                .build());
    }

    @Operation(summary = "Phát bài kế tiếp từ danh sách yêu thích", description = "Phát bài hát tiếp theo trong danh sách yêu thích")
    @GetMapping("/wishlist/next/{songId}")
    public ResponseEntity<ResponseWrapper<?>> playNextFromWishlist(@PathVariable Long songId) {
        SongResponseDTO song = playService.playNextSongFromWishlist(songId);
        return ResponseEntity.ok(ResponseWrapper.builder()
                .data(song)
                .code(HttpStatus.OK.value())
                .status(HttpStatus.OK)
                .build());
    }

    @Operation(summary = "Phát bài trước đó từ danh sách yêu thích", description = "Phát bài hát trước đó trong danh sách yêu thích")
    @GetMapping("/wishlist/prev/{songId}")
    public ResponseEntity<ResponseWrapper<?>> playPrevFromWishlist(@PathVariable Long songId) {
        SongResponseDTO song = playService.playPrevSongFromWishlist(songId);
        return ResponseEntity.ok(ResponseWrapper.builder()
                .data(song)
                .code(HttpStatus.OK.value())
                .status(HttpStatus.OK)
                .build());
    }

    @Operation(summary = "Phát bài theo thể loại", description = "Phát một bài hát cụ thể trong thể loại đã chọn")
    @GetMapping("/genre/{genreId}/song/{songId}")
    public ResponseEntity<ResponseWrapper<?>> playSongByGenre(@PathVariable Long songId, @PathVariable Long genreId) {
        SongResponseDTO song = playService.playSongByGenre(songId, genreId);
        return ResponseEntity.ok(ResponseWrapper.builder()
                .data(song)
                .code(HttpStatus.OK.value())
                .status(HttpStatus.OK)
                .build());
    }

    @Operation(summary = "Phát bài kế tiếp theo thể loại", description = "Phát bài hát tiếp theo trong cùng thể loại")
    @GetMapping("/genre/{genreId}/next/{songId}")
    public ResponseEntity<ResponseWrapper<?>> playNextByGenre(@PathVariable Long songId, @PathVariable Long genreId) {
        SongResponseDTO song = playService.playNextSongByGenre(songId, genreId);
        return ResponseEntity.ok(ResponseWrapper.builder()
                .data(song)
                .code(HttpStatus.OK.value())
                .status(HttpStatus.OK)
                .build());
    }

    @Operation(summary = "Phát bài trước đó theo thể loại", description = "Phát bài hát trước đó trong cùng thể loại")
    @GetMapping("/genre/{genreId}/prev/{songId}")
    public ResponseEntity<ResponseWrapper<?>> playPrevByGenre(@PathVariable Long songId, @PathVariable Long genreId) {
        SongResponseDTO song = playService.playPrevSongByGenre(songId, genreId);
        return ResponseEntity.ok(ResponseWrapper.builder()
                .data(song)
                .code(HttpStatus.OK.value())
                .status(HttpStatus.OK)
                .build());
    }

    @Operation(summary = "Phát bài từ album", description = "Phát một bài hát cụ thể nằm trong album đã chọn")
    @GetMapping("/album/{albumId}/song/{songId}")
    public ResponseEntity<ResponseWrapper<?>> playSongFromAlbum(@PathVariable Long songId, @PathVariable Long albumId) {
        SongResponseDTO song = playService.playSongFromAlbum(songId, albumId);
        return ResponseEntity.ok(ResponseWrapper.builder()
                .data(song)
                .code(HttpStatus.OK.value())
                .status(HttpStatus.OK)
                .build());
    }

    @Operation(summary = "Phát bài kế tiếp trong album", description = "Phát bài tiếp theo trong cùng album")
    @GetMapping("/album/{albumId}/next/{songId}")
    public ResponseEntity<ResponseWrapper<?>> playNextFromAlbum(@PathVariable Long songId, @PathVariable Long albumId) {
        SongResponseDTO song = playService.playNextSongFromAlbum(songId, albumId);
        return ResponseEntity.ok(ResponseWrapper.builder()
                .data(song)
                .code(HttpStatus.OK.value())
                .status(HttpStatus.OK)
                .build());
    }

    @Operation(summary = "Phát bài trước đó trong album", description = "Phát bài trước đó trong cùng album")
    @GetMapping("/album/{albumId}/prev/{songId}")
    public ResponseEntity<ResponseWrapper<?>> playPrevFromAlbum(@PathVariable Long songId, @PathVariable Long albumId) {
        SongResponseDTO song = playService.playPrevSongFromAlbum(songId, albumId);
        return ResponseEntity.ok(ResponseWrapper.builder()
                .data(song)
                .code(HttpStatus.OK.value())
                .status(HttpStatus.OK)
                .build());
    }

    @Operation(summary = "Cập nhật lượt nghe", description = "Cập nhật lượt nghe khi phát được 70% thời lượng bài hát")
    @PostMapping("/song/{songId}/update-views")
    public ResponseEntity<ResponseWrapper<?>> updateSongViews(@PathVariable Long songId, @RequestParam double playedDuration) {
        playService.updateSongViews(songId, playedDuration);
        return ResponseEntity.ok(ResponseWrapper.builder()
                .data("Views updated successfully")
                .code(HttpStatus.OK.value())
                .status(HttpStatus.OK)
                .build());
    }
}