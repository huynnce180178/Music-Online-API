package com.ra.base_spring_boot.controller;

import com.ra.base_spring_boot.dto.ResponseWrapper;
import com.ra.base_spring_boot.dto.req.MoodSuggestRequestDTO;
import com.ra.base_spring_boot.security.principle.MyUserDetails;
import com.ra.base_spring_boot.services.mood.SongSuggestService;
import com.ra.base_spring_boot.services.mood.UserMoodService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Mood Song Controller")
@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
public class MoodSongController {
    @Autowired
    private SongSuggestService songSuggestService;
    @Autowired
    private UserMoodService userMoodService;

    @GetMapping("/MoodSong/hasTodayMood")
    public ResponseEntity<?> hasTodayMood(@AuthenticationPrincipal MyUserDetails userDetails) {
        Long userId = userDetails.getUser().getId();
        return ResponseEntity.ok(ResponseWrapper.builder()
                .data(userMoodService.hasTodayMood(userId))
                .code(HttpStatus.OK.value())
                .status(HttpStatus.OK)
                .build());
    }
    @PostMapping("/MoodSong/suggest")
    public ResponseEntity<?> suggestMoodPlaylist(
            @RequestBody MoodSuggestRequestDTO suggestRequestDTO,
            @AuthenticationPrincipal MyUserDetails userDetails
    ) {
        Long userId = userDetails.getUser().getId();
        String userText = suggestRequestDTO.getUserText();
        songSuggestService.suggestMusicByMood(userId, userText);
        return ResponseEntity.ok(ResponseWrapper.builder()
                .data("Playlist updated based on your mood")
                .code(HttpStatus.OK.value())
                .status(HttpStatus.OK)
                .build());
    }
}
