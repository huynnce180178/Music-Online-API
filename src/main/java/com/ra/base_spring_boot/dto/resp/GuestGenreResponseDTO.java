package com.ra.base_spring_boot.dto.resp;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GuestGenreResponseDTO {
    private Long id;
    private String name;
    private String image;
}
