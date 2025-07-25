package com.ra.base_spring_boot.controller;

import com.ra.base_spring_boot.dto.ResponseWrapper;
import com.ra.base_spring_boot.dto.resp.WishlistResponseDTO;
import com.ra.base_spring_boot.dto.req.WishlistRequestDTO;
import com.ra.base_spring_boot.services.wishlist.WishlistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Wishlist Controller")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class WishlistController {
    @Autowired
    private WishlistService wishlistService;

    @Operation(summary = "Get wishlist", description = "Get the wishlist for the current user with paging and sorting")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Wishlist returned"),
        @ApiResponse(responseCode = "204", description = "No wishlist items found")
    })
    @GetMapping("/user/wishlist")
    public ResponseEntity<?> getWishlist(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "limit", defaultValue = "10") int limit,
            @RequestParam(name = "sortBy", defaultValue = "songId") String sortBy,
            @RequestParam(name = "orderBy", defaultValue = "asc") String orderBy
    ) {
        Sort sort = orderBy.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, limit, sort);
        Page<WishlistResponseDTO> wishlist = wishlistService.getWishlistByCurrentUser(pageable);

        if (wishlist.getTotalElements() == 0) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT).body(
                    ResponseWrapper.builder()
                            .status(HttpStatus.NO_CONTENT)
                            .code(HttpStatus.NO_CONTENT.value())
                            .data("Your wishlist is empty.")
                            .build()
            );
        }

        return ResponseEntity.ok(
                ResponseWrapper.builder()
                        .status(HttpStatus.OK)
                        .code(HttpStatus.OK.value())
                        .data(wishlist)
                        .build()
        );
    }

    @Operation(summary = "Add song to wishlist", description = "Add a song to the current user's wishlist")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Song added to wishlist"),
        @ApiResponse(responseCode = "400", description = "Song already in wishlist or not found")
    })
    @PostMapping("/user/wishlist/add")
    public ResponseEntity<?> addToWishlist(@RequestBody WishlistRequestDTO requestDTO) {
        WishlistResponseDTO response = wishlistService.addToWishlist(requestDTO);
        return ResponseEntity.ok(ResponseWrapper.builder()
                .data(response)
                .code(HttpStatus.OK.value())
                .status(HttpStatus.OK)
                .build());
    }

    @Operation(summary = "Remove song from wishlist", description = "Remove a song from the current user's wishlist")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Song removed from wishlist"),
        @ApiResponse(responseCode = "404", description = "Song not found in wishlist")
    })
    @DeleteMapping("/user/wishlist/remove/{songId}")
    public ResponseEntity<?> removeFromWishlist(@PathVariable Long songId) {
        String result = wishlistService.removeFromWishlist(songId);
        return ResponseEntity.ok(ResponseWrapper.builder()
                .data(result)
                .code(HttpStatus.OK.value())
                .status(HttpStatus.OK)
                .build());
    }
    @Operation(
            summary = "Get number of likes by song id",
            description = "Retrieve the number of users who have added a specific song to their wishlist (liked the song) based on the song ID."
    )
    @GetMapping("/wishlist/countLike/{id}")
    public ResponseEntity<?> countLike(@PathVariable Long id) {
        return ResponseEntity.ok(ResponseWrapper.builder()
                .data(wishlistService.countLikeBySongId(id))
                .code(HttpStatus.OK.value())
                .status(HttpStatus.OK)
                .build());
    }
}
