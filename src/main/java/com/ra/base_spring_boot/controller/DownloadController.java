package com.ra.base_spring_boot.controller;

import com.ra.base_spring_boot.dto.ResponseWrapper;
import com.ra.base_spring_boot.exception.HttpNotFound;
import com.ra.base_spring_boot.services.download.DownloadService;
import com.ra.base_spring_boot.services.download.DownloadServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import com.ra.base_spring_boot.repository.IUserRepository;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Map;

@Tag(name="Download Controller")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class DownloadController {

    private final DownloadService downloadService;
    private final IUserRepository userRepository;

    @Operation(summary = "Download song", description = "Download a song automatically to user's device Downloads folder. Supports Windows, Mac, Android, iPhone with optimized format for each platform.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully downloaded song"),
            @ApiResponse(responseCode = "404", description = "Song not found"),
            @ApiResponse(responseCode = "400", description = "Song not approved for download")
    })
    @PostMapping("/user/downloads/{songId}")
    public ResponseEntity<?> downloadSong(@PathVariable Long songId, HttpServletRequest request) {
        try {
            Long userId = getCurrentUserId();

            String userAgent = request.getHeader("User-Agent");

            // Cast to implementation to access the enhanced method
            if (downloadService instanceof DownloadServiceImpl) {
                ((DownloadServiceImpl) downloadService).downloadSongFileWithUserAgent(userId, songId, userAgent);
            } else {
                downloadService.downloadSongFile(userId, songId);
            }

            // Detect device type for response message
            String deviceInfo = getDeviceInfo(userAgent);

            return ResponseEntity.ok(
                    ResponseWrapper.builder()
                            .data(Map.of(
                                    "message", "File downloaded successfully to Downloads folder",
                                    "deviceType", deviceInfo,
                                    "note", "File format optimized for your device"
                            ))
                            .code(HttpStatus.OK.value())
                            .status(HttpStatus.OK)
                            .build()
            );
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    ResponseWrapper.builder()
                            .data(Map.of("error", e.getMessage()))
                            .code(HttpStatus.BAD_REQUEST.value())
                            .status(HttpStatus.BAD_REQUEST)
                            .build()
            );
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

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new HttpNotFound("User not found"))
                .getId();
    }

    private String getDeviceInfo(String userAgent) {
        if (userAgent != null) {
            userAgent = userAgent.toLowerCase();
            if (userAgent.contains("android")) {
                return "Android (MP3 format)";
            } else if (userAgent.contains("iphone") || userAgent.contains("ipad")) {
                return "iOS (M4A format)";
            } else if (userAgent.contains("mac")) {
                return "macOS (M4A format)";
            } else if (userAgent.contains("windows")) {
                return "Windows (MP3 format)";
            }
        }
        return "Unknown device (MP3 format)";
    }
}
