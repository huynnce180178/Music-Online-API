package com.ra.base_spring_boot.controller.guest;

import com.ra.base_spring_boot.dto.resp.SongResponseDTO;
import com.ra.base_spring_boot.repository.SongRepository;
import com.ra.base_spring_boot.services.banner.BannerService;
import com.ra.base_spring_boot.services.guest.GuestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name="Guest Home Controller")
@RestController
@RequestMapping("/api/v1/guest")
@Validated
public class GuestHomePageController {
    @Autowired
    private GuestService guestService;
    @Autowired
    private BannerService bannerService;
    @Autowired
    private SongRepository songRepository;

    @Operation(
            summary = "Lấy top 10 album nổi bật",
            description = "Trả về danh sách 10 album có lượt xem cao nhất"
    )
    @GetMapping("/Top10Album")
    public ResponseEntity<?> getTop10Album() {
        return ResponseEntity.ok(guestService.findTop10AlbumsByTotalViews());
    }

    @Operation(
            summary = "Lấy top 10 thể loại nổi bật",
            description = "Trả về danh sách 10 thể loại có lượt xem cao nhất"
    )
    @GetMapping("/Top10Genre")
    public ResponseEntity<?> getTop10Genre() {
        return ResponseEntity.ok(guestService.findTop10GenresByTotalViews());
    }

    @Operation(
            summary = "Lấy top 10 nghệ sĩ nổi bật",
            description = "Trả về danh sách 10 nghệ sĩ có lượt xem cao nhất"
    )
    @GetMapping("/Top10Artist")
    public ResponseEntity<?> getTop10Artist() {
        return ResponseEntity.ok(guestService.findTop10ArtistsByTotalViews());
    }

    @Operation(
            summary = "Lấy top bài hát của tuần",
            description = "Trả về danh sách các bài hát có lượt nghe cao nhất trong tuần"
    )
    @GetMapping("/TopSongOfWeak")
    public ResponseEntity<?> getTopSongOfWeak() {
        return ResponseEntity.ok(guestService.findTopSongsOfWeekByTotalViews());
    }

    @Operation(
            summary = "Lấy banner theo vị trí",
            description = "Trả về thông tin banner dựa theo vị trí hiển thị (top, mid, bottom,...)"
    )
    @GetMapping("/banner/{position}")
    public ResponseEntity<?> getBanner(@PathVariable String position) {
        return ResponseEntity.ok(bannerService.findBannerByPosition(position));
    }

    @Operation(
            summary = "Nghe bài hát (stream)",
            description = "Lưu lịch sử nghe và trả về URL file nhạc để phát - chỉ cho phép bài hát đã được approve"
    )
    @GetMapping("/stream/{id}")
    public ResponseEntity<String> streamSong(@PathVariable int id, HttpSession session) {
        var song = songRepository.findById((long) id);
        if (song == null || song.getStatus() != com.ra.base_spring_boot.model.constants.ApprovalStatus.APPROVED) {
            return ResponseEntity.notFound().build();
        }

        guestService.savePlayedSongForGuest(id, session);
        return ResponseEntity.ok(song.getFileUrl());
    }

    @Operation(
            summary = "Lấy danh sách bài hát vừa nghe",
            description = "Trả về danh sách các bài hát người dùng khách vừa phát gần đây"
    )
    @GetMapping("/recent-songs")
    public ResponseEntity<List<SongResponseDTO>> getRecentSongs(HttpSession session) {
        return ResponseEntity.ok(guestService.getRecentlyPlayedSongs(session));
    }

    @Operation(
            summary = "Lấy top 12 bài hát theo lượt xem",
            description = "Trả về danh sách 12 bài hát có lượt xem cao nhất"
    )
    @GetMapping("/TopSongs")
    public ResponseEntity<?> getTop8Songs() {
        return ResponseEntity.ok(guestService.findTopSongsByTotalViews());
    }
    @Operation(
            summary = "Get the top 12 most viewed songs",
            description = "Returns a list of the top 12 songs with the highest views in the last 30 days"
    )
    @GetMapping("/TopSongsInMonth")
    public ResponseEntity<?> getTopSongsInMonth() {
        return ResponseEntity.ok(guestService.getTopSongsInMonth());
    }
}