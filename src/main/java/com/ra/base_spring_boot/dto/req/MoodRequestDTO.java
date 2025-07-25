package com.ra.base_spring_boot.dto.req;

import com.ra.base_spring_boot.model.constants.MoodType;
import jakarta.validation.constraints.*;
import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class MoodRequestDTO {

    @NotNull(message = "Mood type is required")
    private MoodType moodType;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    @NotNull(message = "Intensity is required")
    @Min(value = 1, message = "Intensity must be at least 1")
    @Max(value = 10, message = "Intensity must not exceed 10")
    private Integer intensity;
}