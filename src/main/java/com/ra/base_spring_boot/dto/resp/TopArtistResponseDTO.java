package com.ra.base_spring_boot.dto.resp;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
public class TopArtistResponseDTO {
    private Long artistId;
    private String firstName;
    private String lastName;
    private String profile_image;
    private BigDecimal totalViews;
}
