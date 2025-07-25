package com.ra.base_spring_boot.dto.resp;

import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class MoodTypeResponseDTO {
    
    private Long id;
    private String name;
}