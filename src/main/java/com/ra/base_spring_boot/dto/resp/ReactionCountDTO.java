package com.ra.base_spring_boot.dto.resp;

import com.ra.base_spring_boot.model.constants.ReactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReactionCountDTO {
    private ReactionType type;
    private Long count;
}
