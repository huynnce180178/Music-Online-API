package com.ra.base_spring_boot.controller;

import com.ra.base_spring_boot.dto.ResponseWrapper;
import com.ra.base_spring_boot.dto.resp.UserFollowResponseDTO;
import com.ra.base_spring_boot.security.principle.MyUserDetails;
import com.ra.base_spring_boot.services.user.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name="Follow Controller")
@RestController
@RequestMapping("/api/v1/Follow")
public class FollowController {
    @Autowired
    private UserService userService;

    @Operation(summary = "Get users you are following",
            description = "Returns a list of users that the currently authenticated user is following")
    @GetMapping("/following")
    ResponseEntity<?> getFollowing(@AuthenticationPrincipal MyUserDetails  userDetails) {
        return ResponseEntity.ok(ResponseWrapper.builder()
                .data(userService.getFollowings(userDetails.getUser().getId()))
                .code(HttpStatus.OK.value())
                .status(HttpStatus.OK)
                .build());
    }
    @Operation(summary = "Get users who are following you",
            description = "Returns a list of users who are following the currently authenticated user")
    @GetMapping("/follower")
    ResponseEntity<?> getFollower(@AuthenticationPrincipal MyUserDetails  userDetails) {
        return ResponseEntity.ok(ResponseWrapper.builder()
                .data(userService.getFollowers(userDetails.getUser().getId()))
                .code(HttpStatus.OK.value())
                .status(HttpStatus.OK)
                .build());
    }
    @Operation(summary = "Follow another user",
            description = "The currently authenticated user will follow the target user by their ID")
    @GetMapping("/followUser")
    ResponseEntity<?> followUser(@AuthenticationPrincipal MyUserDetails  userDetails,
                                 @RequestParam Long targetUserId) {
        userService.followUser(userDetails.getUser().getId(),targetUserId);
        return ResponseEntity.ok(ResponseWrapper.builder()
                .data("Successfully followed user with ID " + targetUserId)
                .code(HttpStatus.OK.value())
                .status(HttpStatus.OK)
                .build());
    }
    @Operation(summary = "Unfollow a user",
            description = "The currently authenticated user will unfollow the target user by their ID")
    @GetMapping("/unFollowUser")
    ResponseEntity<?> unFollowUser(@AuthenticationPrincipal MyUserDetails  userDetails,
                                 @RequestParam Long targetUserId) {
        userService.unFollowUser(userDetails.getUser().getId(),targetUserId);
        return ResponseEntity.ok(ResponseWrapper.builder()
                .data("Successfully unfollowed user with ID " + targetUserId)
                .code(HttpStatus.OK.value())
                .status(HttpStatus.OK)
                .build());
    }
    @Operation(summary = "Check if current user is following another user",
            description = "Returns true if the currently authenticated user is following the target user, otherwise false")

    @GetMapping("/isFollowing")
    public ResponseEntity<?> isFollowing(
            @AuthenticationPrincipal MyUserDetails userDetails,
            @RequestParam Long targetUserId) {
        boolean result = userService.isFollowing(userDetails.getUser().getId(), targetUserId);
        return ResponseEntity.ok(ResponseWrapper.builder()
                .data(result)
                .code(HttpStatus.OK.value())
                .status(HttpStatus.OK)
                .build());
    }


}
