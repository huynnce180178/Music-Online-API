package com.ra.base_spring_boot.controller.guest;

import com.ra.base_spring_boot.dto.resp.GenreResponseDTO;
import com.ra.base_spring_boot.dto.resp.GenreWithSongResponseDTO;
import com.ra.base_spring_boot.services.genre.GenreService;
import com.ra.base_spring_boot.services.guest.GuestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name="Guest Genre Page Controller")
@RestController
@RequestMapping("/api/v1/guest/genre")
@Validated
public class GuestGenreController {
    @Autowired
    private GuestService guestService;
    @Autowired
    private GenreService genreService;
    @Operation(
            summary = "Lấy top 10 thể loại nổi bật",
            description = "Trả về danh sách 10 thể loại có lượt xem cao nhất"
    )
    @GetMapping("/TopGenre")
    public ResponseEntity<?> getTopGenre(){
        return ResponseEntity.ok(guestService.findTop10GenresByTotalViews());
    }

    @Operation(
            summary = "Lấy danh sách tất cả thể loại",
            description = "Trả về danh sách thể loại có phân trang, sắp xếp và tìm kiếm theo tên"
    )
    @GetMapping("/genres")
    public ResponseEntity<?> getAllGenres(
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Number of genres per page") @RequestParam(defaultValue = "5") int size,
            @Parameter(description = "Field to sort by") @RequestParam(defaultValue = "id") String sortBy,
            @Parameter(description = "Sort direction: asc or desc") @RequestParam(defaultValue = "asc") String sortDir,
            @Parameter(description = "Search term for genre name") @RequestParam(required = false) String search
    ) {
        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<GenreResponseDTO> genres = genreService.getAllGenres(pageable, search);
        return ResponseEntity.ok(genres.getContent());
    }
    @Operation(
            summary = "Lấy danh sách thể loại kèm bài hát",
            description = "Trả về danh sách thể loại có kèm danh sách bài hát, có phân trang và sắp xếp"
    )
    @GetMapping("/genresViewMore")
    public ResponseEntity<?> genresViewMore(
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Number of genres per page") @RequestParam(defaultValue = "5") int size,
            @Parameter(description = "Field to sort by") @RequestParam(defaultValue = "id") String sortBy,
            @Parameter(description = "Sort direction: asc or desc") @RequestParam(defaultValue = "asc") String sortDir
    ) {
        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<GenreWithSongResponseDTO> genres = genreService.getGenresWithSongs(pageable);
        return ResponseEntity.ok(genres.getContent());
    }
    @Operation(
            summary = "Lấy danh sách bài hát theo ID thể loại",
            description = "Trả về danh sách các bài hát thuộc thể loại được chỉ định"
    )
    @GetMapping("/{genreId}/songs")
    public ResponseEntity<?> getSongsByGenreId(
            @Parameter(description = "Category ID") @PathVariable Long genreId
    ) {
        return ResponseEntity.ok(genreService.getSongsByGenreId(genreId));
    }

}
