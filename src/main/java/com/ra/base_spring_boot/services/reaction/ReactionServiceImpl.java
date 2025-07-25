package com.ra.base_spring_boot.services.reaction;

import com.ra.base_spring_boot.dto.resp.ReactionCountDTO;
import com.ra.base_spring_boot.exception.HttpNotFound;
import com.ra.base_spring_boot.model.Comment;
import com.ra.base_spring_boot.model.Reaction;
import com.ra.base_spring_boot.model.User;
import com.ra.base_spring_boot.model.constants.ReactionType;
import com.ra.base_spring_boot.repository.CommentRepository;
import com.ra.base_spring_boot.repository.ReactionRepository;
import com.ra.base_spring_boot.repository.IUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReactionServiceImpl implements ReactionService {
    @Autowired
    private ReactionRepository reactionRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private IUserRepository userRepository;

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new HttpNotFound("User not found"));
    }

    @Override
    @Transactional
    public void toggleReaction(Long commentId, ReactionType type) {
        User currentUser = getCurrentUser();
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new HttpNotFound("Comment not found"));

        List<Reaction> existingReactions = reactionRepository.findByUserIdAndCommentId(currentUser.getId(), commentId);

        if (!existingReactions.isEmpty()) {
            Reaction existingReaction = existingReactions.get(0);
            if (existingReaction.getType().equals(type)) {
                reactionRepository.deleteById(existingReaction.getId());
            } else {
                existingReaction.setType(type);
                reactionRepository.save(existingReaction);
            }
        } else {
            Reaction newReaction = Reaction.builder()
                    .user(currentUser)
                    .comment(comment)
                    .type(type)
                    .build();
            reactionRepository.save(newReaction);
        }
    }

    @Override
    @Transactional
    public void removeReaction(Long commentId) {
        User currentUser = getCurrentUser();
        List<Reaction> reactions = reactionRepository.findByUserIdAndCommentId(currentUser.getId(), commentId);
        if (!reactions.isEmpty()) {
            reactionRepository.deleteById(reactions.get(0).getId());
        }
    }

    @Override
    public List<ReactionCountDTO> getReactionCounts(Long commentId) {
        List<Object[]> reactionCounts = reactionRepository.countReactionsByCommentId(commentId);
        return reactionCounts.stream()
                .map(result -> ReactionCountDTO.builder()
                        .type((ReactionType) result[0])
                        .count((Long) result[1])
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public String getCurrentUserReaction(Long commentId) {
        try {
            User currentUser = getCurrentUser();
            List<Reaction> reactions = reactionRepository.findByUserIdAndCommentId(currentUser.getId(), commentId);
            return reactions.isEmpty() ? null : reactions.get(0).getType().name();
        } catch (HttpNotFound e) {
            return null;
        }
    }
}
