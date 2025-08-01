package com.ra.base_spring_boot.dto.resp;

import com.ra.base_spring_boot.model.constants.AlbumType;
import com.ra.base_spring_boot.model.constants.ApprovalStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlbumDetailResponseDTO {
    // Thông tin album
    private Long albumId;
    private String title;
    private LocalDateTime releaseDate;
    private String coverImage;
    private AlbumType type;
    private ApprovalStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Thông tin chủ nhân album (artist)
    private AlbumOwnerInfo owner;

    // Thống kê album
    private AlbumStats stats;
}
