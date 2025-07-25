package com.ra.base_spring_boot.controller;

import com.ra.base_spring_boot.dto.ResponseWrapper;
import com.ra.base_spring_boot.services.album.AlbumService;
import com.ra.base_spring_boot.services.comment.CommentService;
import com.ra.base_spring_boot.services.genre.GenreService;
import com.ra.base_spring_boot.services.song.SongService;
import com.ra.base_spring_boot.services.subscription.SubscriptionService;
import com.ra.base_spring_boot.services.user.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@Tag(name = "Statistic Controller", description = "Provides various statistical data for the admin dashboard")
@RestController
@RequestMapping("/api/v1/admin/statistic")
@Validated
public class StatisticController {

    @Autowired
    private UserService userService;

    @Autowired
    private AlbumService albumService;

    @Autowired
    private GenreService genreService;

    @Autowired
    private SongService songService;

    @Autowired
    private CommentService commentService;

    @Autowired
    private SubscriptionService subscriptionService;

    @Operation(summary = "User statistics", description = "Returns total users and number of users by role")
    @GetMapping("/userStatistic")
    public ResponseEntity<?> getUserStatistic(@RequestParam(required = false) LocalDateTime start, @RequestParam(required = false) LocalDateTime end) {
        return buildResponse(userService.getStatisticUserResponseDTO(start, end));
    }

    @Operation(summary = "Album statistics", description = "Returns the total number of albums")
    @GetMapping("/albumStatistic")
    public ResponseEntity<?> getAlbumStatistic(@RequestParam(required = false) LocalDateTime start,@RequestParam(required = false) LocalDateTime end) {
        return buildResponse(albumService.countAlbum(start, end));
    }

    @Operation(summary = "Song statistics", description = "Returns the total number of songs")
    @GetMapping("/songStatistic")
    public ResponseEntity<?> getSongStatistic(@RequestParam(required = false) LocalDateTime start,@RequestParam(required = false) LocalDateTime end) {
        return buildResponse(songService.countSong(start,end));
    }

    @Operation(summary = "Genre statistics", description = "Returns the total number of genres")
    @GetMapping("/genreStatistic")
    public ResponseEntity<?> getGenreStatistic() {
        return buildResponse(genreService.genreCount());
    }

    @Operation(summary = "Subscription statistics", description = "Returns total subscriptions by plan type")
    @GetMapping("/subscriptionStatistic")
    public ResponseEntity<?> getSubscriptionStatistic(@RequestParam(required = false)LocalDateTime start,@RequestParam(required = false)LocalDateTime end) {
        return buildResponse(subscriptionService.getStatisticAllPlan(start, end));
    }

    @Operation(summary = "Comment statistics", description = "Returns the total number of comments")
    @GetMapping("/commentStatistic")
    public ResponseEntity<?> getCommentStatistic(@RequestParam(required = false)LocalDateTime start,@RequestParam(required = false)LocalDateTime end) {
        return buildResponse(commentService.countComment(start, end));
    }

    // ✅ Reusable response wrapper
    private ResponseEntity<?> buildResponse(Object data) {
        return ResponseEntity.ok(
                ResponseWrapper.builder()
                        .data(data)
                        .code(HttpStatus.OK.value())
                        .status(HttpStatus.OK)
                        .build()
        );
    }


}
