package com.ra.base_spring_boot.controller;

import com.ra.base_spring_boot.dto.ResponseWrapper;
import com.ra.base_spring_boot.dto.req.UpdateProfileRequestDTO;
import com.ra.base_spring_boot.dto.resp.ProfileResponseDTO;
import com.ra.base_spring_boot.security.principle.MyUserDetails;
import com.ra.base_spring_boot.services.user.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Tag(name="Profile User Controller")
@RestController
@RequestMapping("/api/v1/user/profile")
@Validated
public class ProfileController {
    @Autowired
    private UserService userService;


    @Operation(
            summary = "Get user profile",
            description = "Fetches the profile of the currently authenticated user based on their ID"
    )
    @GetMapping()
    public ResponseEntity<?> getProfile(@AuthenticationPrincipal MyUserDetails userDetails) {
        Long userID = userDetails.getUser().getId();

        return ResponseEntity.ok(ResponseWrapper.builder()
                        .status(HttpStatus.OK)
                        .code(200)
                        .data(userService.getProfileById(userID))
                .build());
    }
    @Operation(
            summary = "Update user profile",
            description = "Updates the profile information (like first name, last name, and bio) of the currently authenticated user"
    )
    @PostMapping("/update")
    public ResponseEntity<?> updateProfile(@AuthenticationPrincipal MyUserDetails userDetails,
                                           @ModelAttribute UpdateProfileRequestDTO updateProfileRequestDTO) {
        Long userID = userDetails.getUser().getId();
        ProfileResponseDTO profileResponseDTO = userService.updateProfile(userID, updateProfileRequestDTO);
        return ResponseEntity.ok(ResponseWrapper.builder()
        .status(HttpStatus.OK)
                .code(200)
                .data(profileResponseDTO)
                .build());
    }
    @Operation(
            summary = "Change user password",
            description = "Allows the user to change their password by providing the old password and the new one"
    )
    @PatchMapping("/changePassWord")
    public ResponseEntity<?> changePassWord(@AuthenticationPrincipal MyUserDetails userDetails,
                                            @RequestParam String oldPassword,
                                            @RequestParam String newPassword) {
        Long userID = userDetails.getUser().getId();
        userService.changePassWord(userID, oldPassword, newPassword);
        return ResponseEntity.ok(ResponseWrapper.builder()
                .status(HttpStatus.OK)
                .code(200)
                .data("Password changed successfully")
                .build());
    }
    @Operation(
            summary = "Set user password (for accounts without password)",
            description = "Allows the user to set a password if they don't have one yet."
    )
    @PatchMapping("/setPassword")
    public ResponseEntity<?> setPassword(@AuthenticationPrincipal MyUserDetails userDetails,
                                         @RequestParam String newPassword) {
        Long userID = userDetails.getUser().getId();
        userService.setPassword(userID, newPassword);
        return ResponseEntity.ok(ResponseWrapper.builder()
                .status(HttpStatus.OK)
                .code(200)
                .data("Password set successfully")
                .build());
    }
    @Operation(
            summary = "Check if user has password",
            description = "Return true if user has password, false if not."
    )
    @GetMapping("/hasPassword")
    public ResponseEntity<?> hasPassword(@AuthenticationPrincipal MyUserDetails userDetails) {
        Long userID = userDetails.getUser().getId();
        boolean hasPassword = userService.hasPassword(userID);
        return ResponseEntity.ok(ResponseWrapper.builder()
                .status(HttpStatus.OK)
                .code(200)
                .data(hasPassword)
                .build());
    }
}
