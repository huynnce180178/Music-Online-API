package com.ra.base_spring_boot.controller;

import com.ra.base_spring_boot.dto.ResponseWrapper;
import com.ra.base_spring_boot.dto.resp.SongHistoryResponseDTO;
import com.ra.base_spring_boot.services.songhistory.SongHistoryService;
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

@Tag(name = "Song History Controller")
@RestController
@RequestMapping("/api/v1/user/history")
@RequiredArgsConstructor
public class SongHistoryController {
    private final SongHistoryService songHistoryService;

    @Operation(summary = "Get full history", description = "Get the full listening history for a user")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "History returned"),
        @ApiResponse(responseCode = "204", description = "No history found")
    })
    @GetMapping("")
    public ResponseEntity<?> getHistory(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "limit", defaultValue = "12") int limit,
            @RequestParam(name = "sortBy", defaultValue = "playedAt") String sortBy,
            @RequestParam(name = "orderBy", defaultValue = "asc") String orderBy
    ) {
        Sort sort = orderBy.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, limit, sort);
        Page<SongHistoryResponseDTO> history = songHistoryService.getHistoryByCurrentUser(pageable);

        if (history.getTotalElements() == 0) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT).body(
                    ResponseWrapper.builder()
                            .status(HttpStatus.NO_CONTENT)
                            .code(HttpStatus.NO_CONTENT.value())
                            .data("No listening history found.")
                            .build()
            );
        }

        return ResponseEntity.ok(
                ResponseWrapper.builder()
                        .status(HttpStatus.OK)
                        .code(HttpStatus.OK.value())
                        .data(history)
                        .build()
        );
    }

    @Operation(summary = "Remove song from history", description = "Remove a specific song from user's listening history")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Song removed from history"),
        @ApiResponse(responseCode = "404", description = "History entry not found")
    })
    @DeleteMapping("/clear/{historyId}")
    public ResponseEntity<?> clearSongFromHistory(@PathVariable Long historyId) {
        songHistoryService.clearSongFromHistory(historyId);
        return ResponseEntity.ok(ResponseWrapper.builder()
                .data("Song removed from history successfully")
                .code(HttpStatus.OK.value())
                .status(HttpStatus.OK)
                .build());
    }

    @Operation(summary = "Clear all history", description = "Clear all listening history for the current user")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "History cleared"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @DeleteMapping("/clear-all")
    public ResponseEntity<?> clearAllHistoryForUser() {
        songHistoryService.clearAllHistoryForCurrentUser();
        return ResponseEntity.ok(ResponseWrapper.builder()
                .data("All history cleared successfully")
                .code(HttpStatus.OK.value())
                .status(HttpStatus.OK)
                .build());
    }
}
