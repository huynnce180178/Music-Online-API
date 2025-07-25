package com.ra.base_spring_boot.dto.resp;

import com.ra.base_spring_boot.model.constants.UserStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Getter
@Setter
@Builder
public class UserViewArtistResponseDTO {
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String profileImage;
    private String bio;
    private UserStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Set<String> roles;
    private Set<Long> followings;
    private Set<Long> followers;
}

