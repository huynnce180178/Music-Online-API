package com.ra.base_spring_boot.controller;

import com.ra.base_spring_boot.dto.ResponseWrapper; // Import ResponseWrapper
import com.ra.base_spring_boot.dto.req.PlaylistRequestDTO;
import com.ra.base_spring_boot.dto.resp.PlaylistResponseDTO;
import com.ra.base_spring_boot.dto.resp.PublicPlaylistResponseDTO;
import com.ra.base_spring_boot.dto.resp.SongResponseDTO;
import com.ra.base_spring_boot.services.playlist.PlaylistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name="Playlist Controller")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class PlaylistController {
    private final PlaylistService playlistService;

    @Operation(summary = "Tạo danh sách phát mới", description = "Tạo danh sách phát mới cho người dùng")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Successfully created playlist"),
            @ApiResponse(responseCode = "400", description = "Failed to create playlist")
    })
    @PostMapping(path = "/user/playlist/add", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResponseWrapper<?>> createPlaylist(@ModelAttribute PlaylistRequestDTO dto){
        PlaylistResponseDTO created = playlistService.createPlaylist(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ResponseWrapper.builder()
                        .data(created)
                        .code(HttpStatus.CREATED.value())
                        .status(HttpStatus.CREATED)
                        .build()
        );
    }

    @Operation(summary = "Tìm kiếm danh sách phát", description = "Tìm kiếm danh sách phát theo từ khóa")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully found playlists"),
            @ApiResponse(responseCode = "404", description = "No playlists found")
    })
    @GetMapping("/user/playlist/search")
    public ResponseEntity<ResponseWrapper<?>> searchPlaylists(@RequestParam String keyword) {
        List<PlaylistResponseDTO> results = playlistService.searchPlaylists(keyword);
        return ResponseEntity.ok(
                ResponseWrapper.builder()
                        .data(results)
                        .code(HttpStatus.OK.value())
                        .status(HttpStatus.OK)
                        .build()
        );
    }

    @Operation(summary = "Thêm bài hát vào danh sách phát", description = "Thêm bài hát vào danh sách phát")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully added song to playlist"),
            @ApiResponse(responseCode = "400", description = "Failed to add song to playlist")
    })
    @PostMapping("/user/playlist/add-song")
    public ResponseEntity<ResponseWrapper<?>> addSongToPlaylist(@RequestParam Long playlistId, @RequestParam Long songId) {
        PlaylistResponseDTO updated = playlistService.addSongToPlaylist(playlistId, songId);
        return ResponseEntity.ok(
                ResponseWrapper.builder()
                        .data(updated)
                        .code(HttpStatus.OK.value())
                        .status(HttpStatus.OK)
                        .build()
        );
    }

    @Operation(summary = "Xóa bài hát khỏi danh sách phát", description = "Xóa một bài hát khỏi danh sách phát")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully removed song from playlist"),
            @ApiResponse(responseCode = "400", description = "Failed to remove song from playlist")
    })
    @DeleteMapping("/user/playlist/remove-song")
    public ResponseEntity<ResponseWrapper<?>> removeSongFromPlaylist(@RequestParam Long playlistId, @RequestParam Long songId) {
        playlistService.removeSongFromPlaylist(playlistId, songId);
        return ResponseEntity.ok(
                ResponseWrapper.builder()
                        .data("Song removed from playlist successfully")
                        .code(HttpStatus.OK.value())
                        .status(HttpStatus.OK)
                        .build()
        );
    }

    @Operation(summary = "Lấy tất cả danh sách phát", description = "Lấy tất cả danh sách phát cho người dùng")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved playlists"),
            @ApiResponse(responseCode = "404", description = "No playlists found")
    })
    @GetMapping("/user/playlist")
    public ResponseEntity<ResponseWrapper<?>> getAllPlaylists() {
        List<PlaylistResponseDTO> playlists = playlistService.getAllPlaylists();

        String message = null;

        return ResponseEntity.ok(
                ResponseWrapper.builder()
                        .data(playlists)
                        .message(message)
                        .code(HttpStatus.OK.value())
                        .status(HttpStatus.OK)
                        .build()
        );
    }

    @Operation(summary = "Đặt danh sách phát ở chế độ công khai/riêng tư", description = "Đặt danh sách phát ở chế độ công khai hoặc riêng tư")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully updated playlist public status"),
            @ApiResponse(responseCode = "400", description = "Failed to update playlist public status")
    })
    @PutMapping("/user/playlist/set-public/{playlistId}")
    public ResponseEntity<ResponseWrapper<?>> setPlaylistPublic(@PathVariable Long playlistId, @RequestParam boolean isPublic) {
        PlaylistResponseDTO updated = playlistService.setPlaylistPublic(playlistId, isPublic);
        return ResponseEntity.ok(
                ResponseWrapper.builder()
                        .data(updated)
                        .code(HttpStatus.OK.value())
                        .status(HttpStatus.OK)
                        .build()
        );
    }

    @Operation(summary = "Cập nhật danh sách phát", description = "Cập nhật thông tin danh sách phát (tên, mô tả, công khai)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully updated playlist"),
            @ApiResponse(responseCode = "400", description = "Failed to update playlist")
    })
    @PutMapping(path = "/user/playlist/update/{playlistId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResponseWrapper<?>> updatePlaylist(@PathVariable Long playlistId, @ModelAttribute PlaylistRequestDTO dto){
        PlaylistResponseDTO updated = playlistService.updatePlaylist(playlistId, dto);
        return ResponseEntity.ok(
                ResponseWrapper.builder()
                        .data(updated)
                        .code(HttpStatus.OK.value())
                        .status(HttpStatus.OK)
                        .build()
        );
    }

    @Operation(summary = "Xóa danh sách phát", description = "Xóa danh sách phát theo id")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully deleted playlist"),
            @ApiResponse(responseCode = "400", description = "Failed to delete playlist")
    })
    @DeleteMapping("/user/playlist/delete/{playlistId}")
    public ResponseEntity<ResponseWrapper<?>> deletePlaylist(@PathVariable Long playlistId) {
        playlistService.deletePlaylist(playlistId);
        return ResponseEntity.ok(
                ResponseWrapper.builder()
                        .data("Playlist deleted successfully")
                        .code(HttpStatus.OK.value())
                        .status(HttpStatus.OK)
                        .build()
        );
    }

    @Operation(summary = "Lấy danh sách bài hát của playlist", description = "Lấy danh sách bài hát thuộc một playlist cụ thể")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved songs of playlist"),
            @ApiResponse(responseCode = "404", description = "Playlist or songs not found")
    })
    @GetMapping("/user/playlist/{playlistId}/songs")
    public ResponseEntity<ResponseWrapper<?>> getSongsOfPlaylist(@PathVariable Long playlistId) {
        List<SongResponseDTO> songs = playlistService.getSongsByPlaylistId(playlistId);
        return ResponseEntity.ok(
                ResponseWrapper.builder()
                        .data(songs)
                        .code(HttpStatus.OK.value())
                        .status(HttpStatus.OK)
                        .build()
        );
    }


    @Operation(summary = "Lấy tất cả playlist công khai", description = "Lấy tất cả playlist công khai cho guest hoặc mọi người")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved public playlists"),
            @ApiResponse(responseCode = "404", description = "No public playlists found")
    })
    @GetMapping("/guest/public/playlists")
    public ResponseEntity<ResponseWrapper<?>> getAllPublicPlaylists() {
        List<PublicPlaylistResponseDTO> playlists = playlistService.getPublicPlaylists();
        return ResponseEntity.ok(
                ResponseWrapper.builder()
                        .data(playlists)
                        .code(HttpStatus.OK.value())
                        .status(HttpStatus.OK)
                        .build()
        );
    }

    @Operation(summary = "Tạo mã chia sẻ playlist", description = "Sinh mã chia sẻ cho playlist (dán mã này cho bạn bè)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully generated share code"),
            @ApiResponse(responseCode = "400", description = "Failed to generate share code")
    })
    @GetMapping("/user/playlist/share-code")
    public ResponseEntity<ResponseWrapper<?>> generateShareCode(@RequestParam Long playlistId) {
        String code = playlistService.generateShareCode(playlistId);
        return ResponseEntity.ok(
                ResponseWrapper.builder()
                        .data(code)
                        .code(HttpStatus.OK.value())
                        .status(HttpStatus.OK)
                        .build()
        );
    }

    @Operation(summary = "Nhận playlist qua mã chia sẻ", description = "Nhập mã chia sẻ để nhận playlist")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully received shared playlist"),
            @ApiResponse(responseCode = "400", description = "Invalid or already received share code")
    })
    @PostMapping("/user/playlist/receive-share")
    public ResponseEntity<ResponseWrapper<?>> receiveSharedPlaylist(@RequestParam String shareCode) {
        playlistService.receiveSharedPlaylist(shareCode);
        return ResponseEntity.ok(
                ResponseWrapper.builder()
                        .data("Playlist received successfully")
                        .code(HttpStatus.OK.value())
                        .status(HttpStatus.OK)
                        .build()
        );
    }

    @Operation(summary = "Lấy danh sách playlist đã nhận qua chia sẻ", description = "Lấy tất cả playlist đã nhận qua mã chia sẻ")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved shared playlists"),
            @ApiResponse(responseCode = "404", description = "No shared playlists found")
    })
    @GetMapping("/user/playlist/received-shares")
    public ResponseEntity<ResponseWrapper<?>> getReceivedSharedPlaylists() {
        List<PlaylistResponseDTO> playlists = playlistService.getReceivedSharedPlaylists();
        return ResponseEntity.ok(
                ResponseWrapper.builder()
                        .data(playlists)
                        .code(HttpStatus.OK.value())
                        .status(HttpStatus.OK)
                        .build()
        );
    }

    @Operation(summary = "Lấy danh sách bài hát của playlist (guest)", description = "Lấy danh sách bài hát thuộc một playlist công khai cho guest")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved songs of public playlist for guest"),
            @ApiResponse(responseCode = "404", description = "Playlist or songs not found"),
            @ApiResponse(responseCode = "401", description = "Playlist is not public")
    })
    @GetMapping("/guest/playlist/{playlistId}/songs")
    public ResponseEntity<ResponseWrapper<?>> getSongsOfPlaylistForGuest(@PathVariable Long playlistId) {
        List<SongResponseDTO> songs = playlistService.getSongsByPlaylistIdForGuest(playlistId);
        return ResponseEntity.ok(
                ResponseWrapper.builder()
                        .data(songs)
                        .code(HttpStatus.OK.value())
                        .status(HttpStatus.OK)
                        .build()
        );
    }

    @Operation(summary = "Xóa quyền truy cập playlist đã nhận qua chia sẻ", description = "User xóa playlist đã nhận qua mã share (không xóa playlist gốc)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully deleted shared playlist"),
            @ApiResponse(responseCode = "404", description = "Shared playlist not found for this user")
    })
    @DeleteMapping("/user/playlist/received-shares/{playlistId}")
    public ResponseEntity<ResponseWrapper<?>> deleteReceivedSharedPlaylist(@PathVariable Long playlistId) {
        playlistService.deleteReceivedSharedPlaylist(playlistId);
        return ResponseEntity.ok(
                ResponseWrapper.builder()
                        .data("Deleted shared playlist successfully")
                        .code(HttpStatus.OK.value())
                        .status(HttpStatus.OK)
                        .build()
        );
    }

    @Operation(summary = "Lấy playlist theo độ tuổi người dùng", description = "Lấy playlist phù hợp với độ tuổi của người dùng hiện tại")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved playlist by age group"),
        @ApiResponse(responseCode = "404", description = "No playlist found for user's age group")
    })
    @GetMapping("/user/playlist/age-group")
    public ResponseEntity<ResponseWrapper<?>> getPlaylistByCurrentUserAgeGroup() {
        PlaylistResponseDTO playlist = playlistService.getPlaylistByCurrentUserAgeGroup();
        return ResponseEntity.ok(
                ResponseWrapper.builder()
                        .data(playlist)
                        .code(HttpStatus.OK.value())
                        .status(HttpStatus.OK)
                        .build()
        );
    }
}