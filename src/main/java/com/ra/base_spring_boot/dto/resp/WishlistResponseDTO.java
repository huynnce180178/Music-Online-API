package com.ra.base_spring_boot.dto.resp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Time;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class WishlistResponseDTO {
    private Long songId;
    private String title;
    private Time duration;
    private String artistName;
    private String fileUrl;
    private String imageUrl;
}
