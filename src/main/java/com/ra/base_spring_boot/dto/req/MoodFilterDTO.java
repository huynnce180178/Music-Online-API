package com.ra.base_spring_boot.dto.req;

import com.ra.base_spring_boot.model.constants.MoodType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MoodFilterDTO {
    private List<MoodType> moodTypes;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Integer minIntensity;
    private Integer maxIntensity;
}