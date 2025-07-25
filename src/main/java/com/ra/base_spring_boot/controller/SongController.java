package com.ra.base_spring_boot.controller;

import com.ra.base_spring_boot.dto.ResponseWrapper;
import com.ra.base_spring_boot.dto.req.SongRequestDTO;
import com.ra.base_spring_boot.dto.resp.SongResponseDTO;
import com.ra.base_spring_boot.services.song.SongService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

@Tag(name="Song Controller")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class SongController {
    private final SongService songService;

    @Operation(summary = "Thêm bài hát mới", description = "Thêm một bài hát mới vào hệ thống")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Successfully added song"),
        @ApiResponse(responseCode = "400", description = "Failed to add song")
    })
    @PostMapping(path = "/artist/song/add", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> addSong(@ModelAttribute SongRequestDTO songRequestDTO) {
        SongResponseDTO created = songService.addSong(songRequestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ResponseWrapper.builder()
                        .data(created)
                        .code(HttpStatus.CREATED.value())
                        .status(HttpStatus.CREATED)
                        .build()
        );
    }

    @Operation(summary = "Cập nhật bài hát", description = "Cập nhật thông tin bài hát đã tồn tại")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully updated song"),
        @ApiResponse(responseCode = "404", description = "Song not found")
    })
    @PutMapping(path = "/artist/song/{songId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateSong(@PathVariable Long songId, @ModelAttribute SongRequestDTO songRequestDTO) {
        SongResponseDTO updated = songService.updateSong(songId, songRequestDTO);
        return ResponseEntity.ok(
                ResponseWrapper.builder()
                        .data(updated)
                        .code(HttpStatus.OK.value())
                        .status(HttpStatus.OK)
                        .build()
        );
    }

    @Operation(summary = "Xóa bài hát", description = "Chuyển trạng thái bài hát sang DELETED_BY_ARTIST")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully changed song status to deleted"),
        @ApiResponse(responseCode = "404", description = "Song not found")
    })
    @DeleteMapping("/artist/song/delete/{songId}")
    public ResponseEntity<?> deleteSong(@PathVariable Long songId) {
        songService.deleteByArtist(songId);
        return ResponseEntity.ok(
                ResponseWrapper.builder()
                        .data("The song has been marked as deleted. Contact admin for recovery.")
                        .code(HttpStatus.OK.value())
                        .status(HttpStatus.OK)
                        .build()
        );
    }

    @Operation(summary = "Lấy danh sách bài hát trong album", description = "Lấy tất cả bài hát thuộc về một album cụ thể")
    @GetMapping("/artist/song/album/{albumId}")
    public ResponseEntity<?> getSongsByAlbum(@PathVariable Long albumId) {
        List<SongResponseDTO> songs = songService.getSongsByAlbum(albumId);
        return ResponseEntity.ok(
                ResponseWrapper.builder()
                        .data(songs)
                        .code(HttpStatus.OK.value())
                        .status(HttpStatus.OK)
                        .build()
        );
    }

    @Operation(summary = "Xóa bài hát khỏi album", description = "Xóa một bài hát ra khỏi album")
    @DeleteMapping("/artist/song/remove-from-album/{songId}")
    public ResponseEntity<?> removeSongFromAlbum(@PathVariable Long songId) {
        songService.removeSongFromAlbum(songId);
        return ResponseEntity.ok(
                ResponseWrapper.builder()
                        .data("Song removed from album")
                        .code(HttpStatus.OK.value())
                        .status(HttpStatus.OK)
                        .build()
        );
    }

    @Operation(summary = "Thêm bài hát vào album", description = "Thêm một bài hát vào album đã tồn tại")
    @PostMapping("/artist/song/add-to-album")
    public ResponseEntity<?> addSongToAlbum(@RequestParam Long songId, @RequestParam Long albumId) {
        songService.addSongToAlbum(songId, albumId);
        return ResponseEntity.ok(
                ResponseWrapper.builder()
                        .data("Song added to album")
                        .code(HttpStatus.OK.value())
                        .status(HttpStatus.OK)
                        .build()
        );
    }


    @Operation(summary = "Lấy bài hát của tôi", description = "Lấy tất cả bài hát đã được tải lên bởi nghệ sĩ hiện tại")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved your songs"),
        @ApiResponse(responseCode = "401", description = "You do not have permission to view your songs (ROLE_ARTIST required)")
    })
    @GetMapping("/artist/song/my")
    public ResponseEntity<?> getMySongs() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        List<SongResponseDTO> songs = songService.getMySongs(authentication);
        return ResponseEntity.ok(
                ResponseWrapper.builder()
                        .data(songs)
                        .code(HttpStatus.OK.value())
                        .status(HttpStatus.OK)
                        .build()
        );
    }

    @Operation(summary = "Lấy bài hát bị từ chối", description = "Lấy danh sách bài hát bị admin từ chối")
    @GetMapping("/artist/song/my-rejected")
    public ResponseEntity<?> getMyRejectedSongs() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        List<SongResponseDTO> rejectedSongs = songService.getMyRejectedSongs(authentication);
        return ResponseEntity.ok(
                ResponseWrapper.builder()
                        .data(rejectedSongs)
                        .code(HttpStatus.OK.value())
                        .status(HttpStatus.OK)
                        .build()
        );
    }

    @Operation(summary = "Yêu cầu khôi phục bài hát", description = "Yêu cầu admin khôi phục bài hát đã bị từ chối")
    @PostMapping("/artist/song/{songId}/request-restore")
    public ResponseEntity<?> requestRestoreSong(@PathVariable Long songId) {
        try {
            songService.requestRestoreSong(songId);
            return ResponseEntity.ok(
                    ResponseWrapper.builder()
                            .data("Song restoration request has been sent to admin")
                            .code(HttpStatus.OK.value())
                            .status(HttpStatus.OK)
                            .build()
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    ResponseWrapper.builder()
                            .data("Error: " + e.getMessage())
                            .code(HttpStatus.BAD_REQUEST.value())
                            .status(HttpStatus.BAD_REQUEST)
                            .build()
            );
        }
    }

    @Operation(summary = "Lấy bài hát do tôi xóa", description = "Lấy danh sách bài hát đã được tôi tự xóa (xóa mềm)")
    @GetMapping("/artist/song/my-deleted")
    public ResponseEntity<?> getMyDeletedSongs() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        List<SongResponseDTO> deletedSongs = songService.getMySelfDeletedSongs(authentication);
        return ResponseEntity.ok(
                ResponseWrapper.builder()
                        .data(deletedSongs)
                        .code(HttpStatus.OK.value())
                        .status(HttpStatus.OK)
                        .build()
        );
    }

    @Operation(summary = "Lấy bài hát của tôi bị admin xóa", description = "Lấy danh sách bài hát của tôi đã bị admin xóa")
    @GetMapping("/artist/song/my-deleted-by-admin")
    public ResponseEntity<?> getMyDeletedByAdminSongs() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        List<SongResponseDTO> deletedByAdminSongs = songService.getMyDeletedByAdminSongs(authentication);
        return ResponseEntity.ok(
                ResponseWrapper.builder()
                        .data(deletedByAdminSongs)
                        .code(HttpStatus.OK.value())
                        .status(HttpStatus.OK)
                        .build()
        );
    }

    @Operation(summary = "Lấy lời bài hát theo ID", description = "Lấy lời bài hát dựa trên ID của bài hát")
    @GetMapping("/song/lyric/{id}")
    public ResponseEntity<?> getLyricById(@PathVariable Long id) {
        String lyric = songService.getLyricById(id);
        return ResponseEntity.ok(
                ResponseWrapper.builder()
                        .data(lyric)
                        .code(HttpStatus.OK.value())
                        .status(HttpStatus.OK)
                        .build()
        );
    }

    @Operation(summary = "Lấy bài hát theo artistId", description = "Lấy tất cả bài hát thuộc về một nghệ sĩ cụ thể (công khai)")
    @GetMapping("/user/song/artist/{artistId}")
    public ResponseEntity<?> getSongsByArtistId(@PathVariable Long artistId) {
        List<SongResponseDTO> songs = songService.getSongsByArtistId(artistId);
        return ResponseEntity.ok(
                ResponseWrapper.builder()
                        .data(songs)
                        .code(HttpStatus.OK.value())
                        .status(HttpStatus.OK)
                        .build()
        );
    }

    @Operation(summary = "Lấy danh sách bài hát chờ duyệt của tôi", description = "Lấy tất cả bài hát của nghệ sĩ hiện tại đang ở trạng thái chờ duyệt")
    @GetMapping("/artist/song/my-pending")
    public ResponseEntity<?> getMyPendingSongs() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        List<SongResponseDTO> pendingSongs = songService.getMyPendingSongs(authentication);
        return ResponseEntity.ok(
                ResponseWrapper.builder()
                        .data(pendingSongs)
                        .code(HttpStatus.OK.value())
                        .status(HttpStatus.OK)
                        .build()
        );
    }
}
