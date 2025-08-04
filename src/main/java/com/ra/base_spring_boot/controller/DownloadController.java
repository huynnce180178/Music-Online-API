package com.ra.base_spring_boot.controller;

import com.ra.base_spring_boot.dto.ResponseWrapper;
import com.ra.base_spring_boot.dto.req.DownloadRequestDTO;
import com.ra.base_spring_boot.exception.HttpNotFound;
import com.ra.base_spring_boot.model.Song;
import com.ra.base_spring_boot.services.download.DownloadService;
import com.ra.base_spring_boot.repository.SongRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import com.ra.base_spring_boot.repository.IUserRepository;

import java.util.Map;

@Tag(name="Download Controller")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class DownloadController {

    private final DownloadService downloadService;
    private final IUserRepository userRepository;
    private final SongRepository songRepository;

    @Operation(summary = "Download song", description = "Download a song file")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully downloaded song"),
            @ApiResponse(responseCode = "404", description = "Song not found")
    })
    @GetMapping("/user/downloads/{songId}")
    public ResponseEntity<?> downloadSong(@PathVariable Long songId, HttpServletRequest request) {
        try {
            Long userId = getCurrentUserId();

            Song song = songRepository.findById(songId)
                    .orElseThrow(() -> new HttpNotFound("Song not found"));

            byte[] fileData = downloadService.downloadSongFile(userId, songId);

            HttpHeaders headers = downloadService.createDownloadHeaders(song);

            String userAgent = request.getHeader("User-Agent");
            if (userAgent != null) {
                userAgent = userAgent.toLowerCase();

                // Tối ưu cho Safari và iOS
                if (userAgent.contains("safari") && !userAgent.contains("chrome")) {
                    headers.add("Content-Description", "File Transfer");
                    headers.add("Expires", "0");
                    headers.add("Cache-Control", "no-cache, no-store, must-revalidate");
                    headers.add("Pragma", "no-cache");
                }

                // Tối ưu cho Android
                if (userAgent.contains("android")) {
                    headers.add("Content-Description", "File Transfer");
                    headers.add("Content-Transfer-Encoding", "binary");
                }

                // Tối ưu cho iOS Safari
                if (userAgent.contains("iphone") || userAgent.contains("ipad")) {
                    headers.add("Content-Description", "File Transfer");
                    headers.add("Accept-Ranges", "bytes");
                    headers.add("Cache-Control", "no-cache, no-store, must-revalidate");
                }

                // Tối ưu cho Chrome trên mobile
                if (userAgent.contains("chrome") && (userAgent.contains("mobile") || userAgent.contains("android"))) {
                    headers.add("Content-Transfer-Encoding", "binary");
                    headers.add("Accept-Ranges", "bytes");
                }

                // Tối ưu cho Firefox
                if (userAgent.contains("firefox")) {
                    headers.add("Content-Transfer-Encoding", "binary");
                    headers.add("Accept-Ranges", "bytes");
                }
            }

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(fileData);

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ResponseWrapper.builder()
                            .data("Download failed: " + e.getMessage())
                            .code(HttpStatus.BAD_REQUEST.value())
                            .status(HttpStatus.BAD_REQUEST)
                            .build());
        }
    }

    @Operation(summary = "Record download", description = "Record a song download in user's download list")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Download recorded successfully"),
            @ApiResponse(responseCode = "404", description = "Song not found")
    })
    @PostMapping("/user/downloads/{songId}/record")
    public ResponseEntity<?> recordDownload(@PathVariable Long songId) {
        try {
            Long userId = getCurrentUserId();
            downloadService.downloadSong(userId, DownloadRequestDTO.builder()
                    .songId(songId)
                    .build());

            return ResponseEntity.ok(
                    ResponseWrapper.builder()
                            .data("Download recorded successfully")
                            .code(HttpStatus.OK.value())
                            .status(HttpStatus.OK)
                            .build()
            );
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ResponseWrapper.builder()
                            .data("Failed to record download: " + e.getMessage())
                            .code(HttpStatus.BAD_REQUEST.value())
                            .status(HttpStatus.BAD_REQUEST)
                            .build());
        }
    }

    @Operation(summary = "Remove download", description = "Remove a song from user's download list")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Song removed from downloads successfully")
    })
    @DeleteMapping("/user/downloads/{songId}")
    public ResponseEntity<?> removeFromDownloads(@PathVariable Long songId) {
        downloadService.deleteDownload(getCurrentUserId(), songId);
        return ResponseEntity.ok(
                ResponseWrapper.builder()
                        .data("Song removed from downloads successfully")
                        .code(HttpStatus.OK.value())
                        .status(HttpStatus.OK)
                        .build()
        );
    }

    @Operation(summary = "Get user downloads with paging", description = "Get user's downloads with pagination and sorting")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved downloads"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @GetMapping("/user/downloads/paging")
    public ResponseEntity<?> getDownloadsWithPaging(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "addedAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {

        Sort.Direction sortDirection = Sort.Direction.fromString(direction.toUpperCase());
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));

        Page<?> downloads = downloadService.getDownloadsByUserWithPaging(getCurrentUserId(), pageable);

        return ResponseEntity.ok(
                ResponseWrapper.builder()
                        .data(downloads)
                        .code(HttpStatus.OK.value())
                        .status(HttpStatus.OK)
                        .build()
        );
    }

    @Operation(summary = "Get download info", description = "Get song info for user to choose where to save - no automatic path")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved download info"),
            @ApiResponse(responseCode = "404", description = "Song not found")
    })
    @GetMapping("/user/downloads/{songId}/info")
    public ResponseEntity<?> getDownloadInfo(@PathVariable Long songId) {
        try {
            Long userId = getCurrentUserId();

            Song song = songRepository.findById(songId)
                    .orElseThrow(() -> new HttpNotFound("Song not found"));

            if (song.getStatus() != com.ra.base_spring_boot.model.constants.ApprovalStatus.APPROVED) {
                throw new RuntimeException("Only approved songs can be downloaded");
            }

            String fileName = sanitizeFileName(song.getTitle()) + ".mp3";

            return ResponseEntity.ok(
                    ResponseWrapper.builder()
                            .data(Map.of(
                                    "songId", songId,
                                    "fileName", fileName,
                                    "songTitle", song.getTitle(),
                                    "albumName", song.getAlbums() != null ? song.getAlbums().getTitle() : "",
                                    "artistName", song.getUsers() != null ?
                                            (song.getUsers().getFirstName() + " " + song.getUsers().getLastName()).trim() : "",
                                    "duration", song.getDuration().toString(),
                                    "message", "Chọn nơi lưu file để tải xuống"
                            ))
                            .code(HttpStatus.OK.value())
                            .status(HttpStatus.OK)
                            .build()
            );

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ResponseWrapper.builder()
                            .data("Failed to get download info: " + e.getMessage())
                            .code(HttpStatus.BAD_REQUEST.value())
                            .status(HttpStatus.BAD_REQUEST)
                            .build());
        }
    }

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new HttpNotFound("User not found"))
                .getId();
    }


    private String sanitizeFileName(String fileName) {
        if (fileName == null) return "song";

        return fileName.replaceAll("[<>:\"/\\\\|?*]", "_")
                .replaceAll("[\\x00-\\x1f\\x7f]", "")
                .trim();
    }
}
