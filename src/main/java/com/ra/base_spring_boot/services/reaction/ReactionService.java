package com.ra.base_spring_boot.services.reaction;

import com.ra.base_spring_boot.dto.resp.ReactionCountDTO;
import com.ra.base_spring_boot.model.constants.ReactionType;
import java.util.List;

public interface ReactionService {
    void toggleReaction(Long commentId, ReactionType type);
    void removeReaction(Long commentId);
    List<ReactionCountDTO> getReactionCounts(Long commentId);
    String getCurrentUserReaction(Long commentId);
}
