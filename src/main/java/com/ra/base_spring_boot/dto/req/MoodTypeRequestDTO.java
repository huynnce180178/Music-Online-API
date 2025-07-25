package com.ra.base_spring_boot.dto.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class MoodTypeRequestDTO {
    
    @NotBlank(message = "Mood type name is required")
    @Size(min = 2, max = 50, message = "Mood type name must be between 2 and 50 characters")
    private String name;
}