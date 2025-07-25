package com.ra.base_spring_boot.controller.guest;

import com.ra.base_spring_boot.dto.ResponseWrapper;
import com.ra.base_spring_boot.dto.resp.ArtistResponseDTO;
import com.ra.base_spring_boot.services.guest.GuestService;
import com.ra.base_spring_boot.services.user.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Guest Artist Page Controller")
@RestController
@RequestMapping("/api/v1/guest/artist")
@Validated
public class GuestArtistController {

    @Autowired
    private GuestService guestService;

    @Autowired
    private UserService userService;

    @Operation(
            summary = "Lấy danh sách nghệ sĩ nổi bật",
            description = "Trả về danh sách top 10 nghệ sĩ có lượt xem nhiều nhất, được hiển thị nổi bật cho người dùng khách"
    )
    @GetMapping("/featuredArtist")
    public ResponseEntity<?> getFeaturedArtist() {
        return ResponseEntity.ok(guestService.findTop10ArtistsByTotalViews());
    }

    @Operation(
            summary = "Lấy danh sách nghệ sĩ với phân trang và tìm kiếm",
            description = "Trả về danh sách nghệ sĩ với các tùy chọn phân trang, sắp xếp, và tìm kiếm theo tên hoặc từ khóa"
    )
    @GetMapping("/listArtist")
    public ResponseEntity<?> getListArtist(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "limit", defaultValue = "10") int limit,
            @RequestParam(name = "sortBy", defaultValue = "id") String sortBy,
            @RequestParam(name = "orderBy", defaultValue = "asc") String orderBy,
            @RequestParam(name = "keyword", required = false) String keyword
    ) {
        Sort sort = orderBy.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, limit, sort);
        Page<ArtistResponseDTO> artist = userService.getArtist(keyword, pageable);
        if (artist.getTotalElements() > 0) {
            return new ResponseEntity<>(artist, HttpStatus.OK);
        }
        return new ResponseEntity<>(ResponseWrapper.builder()
                .status(HttpStatus.NO_CONTENT)
                .code(200)
                .data("There are no artist currently.")
                .build(), HttpStatus.NO_CONTENT);
    }
}
