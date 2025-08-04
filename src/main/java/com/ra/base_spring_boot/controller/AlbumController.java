package com.ra.base_spring_boot.controller;

import com.ra.base_spring_boot.dto.ResponseWrapper;
import com.ra.base_spring_boot.dto.req.AlbumRequestDTO;
import com.ra.base_spring_boot.dto.resp.AlbumDetailResponseDTO;
import com.ra.base_spring_boot.dto.resp.AlbumResponseDTO;
import com.ra.base_spring_boot.services.album.AlbumService;
import com.ra.base_spring_boot.services.song.SongService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.thymeleaf.spring6.SpringTemplateEngine;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name="Album Controller")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class AlbumController {
    private final AlbumService albumService;
    private final SongService songService;
    private final SpringTemplateEngine templateEngine;

    @Operation(summary = "Thêm album", description = "Thêm một album mới (dành cho ARTIST)")
    @PostMapping("/artist/album/add")
    public ResponseEntity<?> createAlbum(@ModelAttribute AlbumRequestDTO albumRequestDTO) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        AlbumResponseDTO created = albumService.createAlbum(albumRequestDTO, authentication);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ResponseWrapper.builder()
                        .data(created)
                        .code(HttpStatus.CREATED.value())
                        .status(HttpStatus.CREATED)
                        .build()
        );
    }

    @Operation(summary = "Cập nhật album", description = "Cập nhật thông tin album (dành cho ARTIST)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully updated album"),
            @ApiResponse(responseCode = "404", description = "Album not found")
    })
    @PutMapping("/artist/album/update/{albumId}")
    public ResponseEntity<?> updateAlbum(@PathVariable Long albumId, @ModelAttribute AlbumRequestDTO albumRequestDTO) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        AlbumResponseDTO updated = albumService.updateAlbum(albumId, albumRequestDTO, authentication);
        return ResponseEntity.ok(
                ResponseWrapper.builder()
                        .data(updated)
                        .code(HttpStatus.OK.value())
                        .status(HttpStatus.OK)
                        .build()
        );
    }

    @Operation(summary = "Xóa album", description = "Chuyển trạng thái album sang DELETED_BY_ARTIST")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully changed album status to deleted"),
            @ApiResponse(responseCode = "404", description = "Album not found")
    })
    @DeleteMapping("/artist/album/delete/{albumId}")
    public ResponseEntity<?> deleteAlbum(@PathVariable Long albumId) {
        albumService.deleteAlbum(albumId);

        return ResponseEntity.ok(
                ResponseWrapper.builder()
                        .data("The album has been marked as deleted. Contact admin for recovery.")
                        .code(HttpStatus.OK.value())
                        .status(HttpStatus.OK)
                        .build()
        );
    }

    @Operation(summary = "Lấy album theo ID nghệ sĩ", description = "Lấy danh sách album theo ID nghệ sĩ (công khai)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully found albums"),
            @ApiResponse(responseCode = "404", description = "No albums found for this artist")
    })
    @GetMapping("/artist/album/{artistId}")
    public ResponseEntity<?> getAlbumsByArtistId(@PathVariable Long artistId) {
        List<AlbumResponseDTO> albums = albumService.getAlbumsByArtistId(artistId);
        return ResponseEntity.ok(
                ResponseWrapper.builder()
                        .data(albums)
                        .code(HttpStatus.OK.value())
                        .status(HttpStatus.OK)
                        .build()
        );
    }

    @Operation(summary = "Tìm kiếm album theo tiêu đề", description = "Tìm kiếm album theo tiêu đề (công khai)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully found albums"),
            @ApiResponse(responseCode = "404", description = "No albums found")
    })
    @GetMapping("/search")
    public ResponseEntity<?> searchAlbumsByTitle(@RequestParam String title) {
        List<AlbumResponseDTO> results = albumService.searchAlbumsByTitle(title);
        return ResponseEntity.ok(
                ResponseWrapper.builder()
                        .data(results)
                        .code(HttpStatus.OK.value())
                        .status(HttpStatus.OK)
                        .build()
        );
    }

    @Operation(summary = "Lấy tất cả album", description = "Lấy tất cả album (dành cho admin hoặc công khai)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved albums"),
            @ApiResponse(responseCode = "404", description = "No albums available")
    })
    @GetMapping("/user/album")
    public ResponseEntity<?> getAllAlbums() {
        List<AlbumResponseDTO> albums = albumService.getAllAlbums();
        return ResponseEntity.ok(
                ResponseWrapper.builder()
                        .data(albums)
                        .code(HttpStatus.OK.value())
                        .status(HttpStatus.OK)
                        .build()
        );
    }

    @Operation(summary = "Lấy album bị từ chối", description = "Lấy danh sách album bị admin từ chối")
    @GetMapping("/artist/album/my-rejected")
    public ResponseEntity<?> getMyRejectedAlbums() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        List<AlbumResponseDTO> rejectedAlbums = albumService.getMyRejectedAlbums(authentication);
        return ResponseEntity.ok(
                ResponseWrapper.builder()
                        .data(rejectedAlbums)
                        .code(HttpStatus.OK.value())
                        .status(HttpStatus.OK)
                        .build()
        );
    }

    @Operation(summary = "Yêu cầu khôi phục album", description = "Yêu cầu admin khôi phục album đã bị từ chối")
    @PostMapping("/artist/album/{albumId}/request-restore")
    public ResponseEntity<?> requestRestoreAlbum(@PathVariable Long albumId) {
        try {
            albumService.requestRestoreAlbum(albumId);
            return ResponseEntity.ok(
                    ResponseWrapper.builder()
                            .data("Album restoration request has been sent to admin")
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

    @Operation(summary = "Lấy album đã xóa của tôi", description = "Lấy danh sách album đã bị xóa bởi nghệ sĩ (xóa mềm)")
    @GetMapping("/artist/album/my-deleted")
    public ResponseEntity<?> getMyDeletedAlbums() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        List<AlbumResponseDTO> deletedAlbums = albumService.getMyDeletedAlbums(authentication);
        return ResponseEntity.ok(
                ResponseWrapper.builder()
                        .data(deletedAlbums)
                        .code(HttpStatus.OK.value())
                        .status(HttpStatus.OK)
                        .build()
        );
    }

    @Operation(summary = "Lấy album của tôi bị admin xóa", description = "Lấy danh sách album của tôi đã bị admin xóa")
    @GetMapping("/artist/album/my-deleted-by-admin")
    public ResponseEntity<?> getMyDeletedByAdminAlbums() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        List<AlbumResponseDTO> deletedByAdminAlbums = albumService.getMyDeletedByAdminAlbums(authentication);
        return ResponseEntity.ok(
                ResponseWrapper.builder()
                        .data(deletedByAdminAlbums)
                        .code(HttpStatus.OK.value())
                        .status(HttpStatus.OK)
                        .build()
        );
    }

    @Operation(summary = "Lấy album theo artistId", description = "Lấy tất cả bài hát thuộc về các album của một nghệ sĩ (user) ")
    @GetMapping("/user/albums/{artistId}")
    public ResponseEntity<?> getSongsByAlbumsOfArtist(@PathVariable Long artistId) {
        List<AlbumResponseDTO> albums = albumService.getAlbumsByArtistId(artistId);
        return ResponseEntity.ok(
                ResponseWrapper.builder()
                        .data(albums)
                        .code(HttpStatus.OK.value())
                        .status(HttpStatus.OK)
                        .build()
        );
    }

    @Operation(summary = "Lấy danh sách album chờ duyệt của tôi", description = "Lấy tất cả album của nghệ sĩ hiện tại đang ở trạng thái chờ duyệt")
    @GetMapping("/artist/albums/my-pending")
    public ResponseEntity<?> getMyPendingAlbums() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        List<AlbumResponseDTO> pendingAlbums = albumService.getMyPendingAlbums(authentication);
        return ResponseEntity.ok(
                ResponseWrapper.builder()
                        .data(pendingAlbums)
                        .code(HttpStatus.OK.value())
                        .status(HttpStatus.OK)
                        .build()
        );
    }

    @Operation(summary = "Lấy thông tin chi tiết album và chủ nhân",
            description = "Lấy thông tin chi tiết album bao gồm thông tin chủ nhân album và các thống kê")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved album details"),
            @ApiResponse(responseCode = "404", description = "Album not found")
    })
    @GetMapping("/guest/albums/{albumId}/details")
    public ResponseEntity<?> getAlbumDetailWithOwner(@PathVariable Long albumId) {
        try {
            AlbumDetailResponseDTO albumDetail = albumService.getAlbumDetailWithOwner(albumId);

            return ResponseEntity.ok(
                    ResponseWrapper.builder()
                            .data(albumDetail)
                            .code(HttpStatus.OK.value())
                            .status(HttpStatus.OK)
                            .message("Album details retrieved successfully")
                            .build()
            );

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ResponseWrapper.builder()
                            .data(null)
                            .code(HttpStatus.NOT_FOUND.value())
                            .status(HttpStatus.NOT_FOUND)
                            .message("Album not found: " + e.getMessage())
                            .build()
            );
        }
    }
}
