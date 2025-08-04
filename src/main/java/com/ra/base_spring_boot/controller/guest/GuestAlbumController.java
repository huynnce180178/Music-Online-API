package com.ra.base_spring_boot.controller.guest;


import com.ra.base_spring_boot.dto.ResponseWrapper;
import com.ra.base_spring_boot.dto.resp.AlbumInfoResponseDTO;
import com.ra.base_spring_boot.dto.resp.SongResponseDTO;
import com.ra.base_spring_boot.services.album.AlbumService;
import com.ra.base_spring_boot.services.guest.GuestService;
import com.ra.base_spring_boot.services.song.SongService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Guest Album")
@RestController
@RequestMapping("/api/v1/guest/ablum")
@Validated
public class GuestAlbumController {

    @Autowired
    private GuestService guestService;
    @Autowired
    private AlbumService albumService;


    @Operation(
            summary = "Lấy tất cả album",
            description = "Trả về danh sách toàn bộ các album hiện có trong hệ thống"
    )
    @GetMapping("/all")
    public ResponseEntity<?> getAllAlbums() {
        return ResponseEntity.ok(guestService.getAllAlbums());
    }

    @Operation(
            summary = "Lấy danh sách album nổi bật",
            description = "Trả về danh sách các album được đánh dấu là nổi bật để hiển thị cho người dùng khách"
    )
    @GetMapping("/featured")
    public ResponseEntity<?> getFeaturedAlbums() {
        return ResponseEntity.ok(guestService.getFeaturedAlbums());
    }

    @Operation(
            summary = "Lấy top 15 album",
            description = "Trả về danh sách 15 album được yêu thích hoặc nghe nhiều nhất trong tuần"
    )
    @GetMapping("/top15")
    public ResponseEntity<?> getTop15Albums() {
        return ResponseEntity.ok(guestService.getTop15Albums());
    }

    @Operation(
            summary = "Lấy album theo nghệ sĩ",
            description = "Trả về danh sách các album thuộc về một nghệ sĩ cụ thể, dựa trên ID của nghệ sĩ"
    )
    @GetMapping("/artist/{artistId}")
    public ResponseEntity<?> getAlbumsByArtist(@PathVariable Long artistId) {
        return ResponseEntity.ok(guestService.getAlbumsByArtist(artistId));
    }

    @GetMapping("/{albumId}/songs")
    public ResponseEntity<?> getAlbumDetail(@PathVariable Long albumId) {
        AlbumInfoResponseDTO albumInfo = albumService.getAlbumInfo(albumId);
        return ResponseEntity.ok(
                ResponseWrapper.builder()
                        .data(albumInfo)
                        .code(HttpStatus.OK.value())
                        .status(HttpStatus.OK)
                        .build()
        );
    }
    @Operation(
            summary = "Lấy mỗi nghệ sĩ 1 album nổi bật nhất",
            description = "Trả về danh sách mỗi nghệ sĩ có 1 album nổi bật nhất dựa vào lượt nghe"
    )
    @GetMapping("/top-one-each-artist")
    public ResponseEntity<?> getOneTopAlbumPerArtist() {
        return ResponseEntity.ok(guestService.getOneTopAlbumPerArtist());
    }

    @Operation(
            summary = "Lấy 8 album mới phát hành",
            description = "Trả về danh sách 8 album mới được phát hành gần đây nhất, sắp xếp theo thời gian tạo"
    )
    @GetMapping("/new-releases")
    public ResponseEntity<?> getNewlyReleasedAlbums() {
        return ResponseEntity.ok(guestService.getNewlyReleasedAlbums());
    }

}
