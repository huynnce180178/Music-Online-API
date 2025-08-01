package com.ra.base_spring_boot.dto.resp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlbumOwnerInfo {
    private Long userId;
    private String fullName;
    private String firstName;
    private String lastName;
    private String email;
    private String profileImage;
    private String bio;
    private Integer age;
    private LocalDateTime joinedDate;

    // Thống kê của artist
    private Integer totalAlbums;
    private Integer totalSongs;
    private Integer totalFollowers;
}
