package com.ra.base_spring_boot.dto.resp;

import com.ra.base_spring_boot.model.constants.ReactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReactionNotificationDTO {
    private Long commentId;
    private Long userId;
    private String username;
    private ReactionType type;
    private Boolean isAdd;
    private List<ReactionCountDTO> updatedReactionCounts;
}
