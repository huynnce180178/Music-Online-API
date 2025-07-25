package com.ra.base_spring_boot.controller;

import com.ra.base_spring_boot.dto.ResponseWrapper;
import com.ra.base_spring_boot.dto.resp.ArtistResponseDTO;
import com.ra.base_spring_boot.dto.resp.UserResponseDTO;
import com.ra.base_spring_boot.dto.resp.UserViewArtistResponseDTO;
import com.ra.base_spring_boot.model.Role;
import com.ra.base_spring_boot.model.User;
import com.ra.base_spring_boot.model.constants.UserStatus;
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
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@Tag(name="User Controller")
@RestController
@RequestMapping("/api/v1")
@Validated
public class UserController {
    @Autowired
    private UserService userService;

    @Operation(summary = "Get all users", description = "Retrieve a list of users with optional filters like search keyword and sorting")
    @GetMapping("/admin/user")
    public ResponseEntity<?> getUsers(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "limit", defaultValue = "10") int limit,
            @RequestParam(name = "sortBy", defaultValue = "id") String sortBy,
            @RequestParam(name = "orderBy", defaultValue = "asc") String orderBy,
            @RequestParam(name = "keyword", required = false) String keyword
    ) {
        Sort sort = orderBy.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, limit, sort);
        Page<UserResponseDTO> users = userService.getUsers(keyword, pageable);
        if (users.getTotalElements() > 0) {
            return new ResponseEntity<>(users, HttpStatus.OK);
        }
        return new ResponseEntity<>(ResponseWrapper.builder()
                .status(HttpStatus.NO_CONTENT)
                .code(200)
                .data("There are no users currently.")
                .build(), HttpStatus.NO_CONTENT);
    }
    @Operation(summary = "Change user status", description = "Change the status of a user (e.g., active, inactive) based on the user ID")
    @PatchMapping("/admin/user/changeStatus/{id}")
    public ResponseEntity<?> changeStatus(@RequestParam UserStatus status, @PathVariable Long id) {
        userService.changeStatus(id, status);
        return ResponseEntity.ok(ResponseWrapper.builder()
                .status(HttpStatus.OK)
                .code(200)
                .data("Chagne Status successfully changed to " + status)
                .build());
    }

    @Operation(summary = "Get artist info for user view by ID", description = "Retrieve detailed artist information for users to view by artist ID.")
    @GetMapping("user/artist/view/{id}")
    public ResponseEntity<?> getUserViewArtistById(@PathVariable Long id) {
        try {
            UserViewArtistResponseDTO dto = userService.getUserViewArtistById(id);
            return ResponseEntity.ok(ResponseWrapper.builder()
                    .status(HttpStatus.OK)
                    .code(200)
                    .data(dto)
                    .build());
        } catch (RuntimeException e) {
            String message = e.getMessage() != null && e.getMessage().contains("not an artist")
                    ? "This user is not an artist."
                    : e.getMessage();
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ResponseWrapper.builder()
                    .status(HttpStatus.NOT_FOUND)
                    .code(404)
                    .data(message)
                    .build());
        }
    }
}
