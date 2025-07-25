package com.ra.base_spring_boot.dto.resp;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class MoodResponseDTO {
    
    private Long id;
    private Long moodTypeId;
    private String moodTypeName;
    private Integer intensity;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String createdAtStr;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String updatedAtStr;
    private Long userId;
    private String userFullName;
}