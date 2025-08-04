package com.ra.base_spring_boot.services.comment;

import com.ra.base_spring_boot.dto.req.CommentRequestDTO;
import com.ra.base_spring_boot.dto.resp.CommentResponseDTO;
import com.ra.base_spring_boot.dto.resp.ReactionCountDTO;
import com.ra.base_spring_boot.exception.HttpNotFound;
import com.ra.base_spring_boot.model.Comment;
import com.ra.base_spring_boot.model.Reaction;
import com.ra.base_spring_boot.model.Song;
import com.ra.base_spring_boot.model.User;
import com.ra.base_spring_boot.model.constants.ReactionType;
import com.ra.base_spring_boot.repository.*;
import com.ra.base_spring_boot.dto.req.NotificationRequest;
import com.ra.base_spring_boot.services.notification.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class CommentServiceImpl implements CommentService {
    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private SongRepository songRepository;

    @Autowired
    private IUserRepository userRepository;

    @Autowired
    private ReactionRepository reactionRepository;

    @Autowired
    private NotificationService notificationService;

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal())) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof UserDetails userDetails) {
                String username = userDetails.getUsername();
                return userRepository.findByEmail(username)
                        .orElseThrow(() -> new HttpNotFound("User not found"));
            } else if (principal instanceof User user) {
                return user;
            }
        }
        throw new HttpNotFound("User not found");
    }

    @Override
    public Long countComment(LocalDateTime start, LocalDateTime end) {
        if (start == null && end == null) {
            return commentRepository.count();
        } else if (start.isAfter(end)) {
            throw new IllegalArgumentException("start time is after end time");
        }
        return commentRepository.countCommentByDate(start, end);
    }

    @Override
    public CommentResponseDTO createComment(CommentRequestDTO commentRequestDTO) {
        User currentUser = getCurrentUser();
        Song song = songRepository.findById(commentRequestDTO.getSongId())
                .orElseThrow(() -> new HttpNotFound("Song not found"));

        Comment comment = Comment.builder()
                .users(currentUser)
                .songs(song)
                .content(commentRequestDTO.getContent())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        if (commentRequestDTO.getParentId() != null) {
            Comment parentComment = commentRepository.findById(commentRequestDTO.getParentId())
                    .orElseThrow(() -> new HttpNotFound("Parent comment not found"));
            comment.setParentComment(parentComment);
        }

        Comment savedComment = commentRepository.save(comment);

        if (comment.getParentComment() != null) {
            User parentUser = comment.getParentComment().getUsers();
            if (parentUser != null && !parentUser.getId().equals(currentUser.getId())) {
                String message = String.format(
                    "Someone replied to your comment on the song '%s'.",
                    song.getTitle()
                );
                notificationService.createNotification(
                    new NotificationRequest(
                        parentUser.getId(),
                        message,
                        "reply_comment"
                    )
                );
            }
        }

        return mapToDTO(savedComment);
    }

    @Override
    public List<CommentResponseDTO> getCommentsBySongId(Long songId) {
        List<Comment> rootComments = commentRepository.findBySongsIdAndParentCommentIsNullOrderByCreatedAtDesc(songId);
        return rootComments.stream()
                .map(comment -> mapToDTOWithReplies(comment))
                .collect(Collectors.toList());
    }

    private List<ReactionCountDTO> getReactionCounts(Long commentId) {
        List<Object[]> reactionCounts = reactionRepository.countReactionsByCommentId(commentId);
        return reactionCounts.stream()
                .map(result -> ReactionCountDTO.builder()
                        .type((ReactionType) result[0])
                        .count((Long) result[1])
                        .build())
                .collect(Collectors.toList());
    }

    private String getCurrentUserReaction(Long commentId) {
        try {
            User currentUser = getCurrentUser();
            List<Reaction> reactions = reactionRepository.findByUserIdAndCommentId(currentUser.getId(), commentId);
            return reactions.isEmpty() ? null : reactions.get(0).getType().name();
        } catch (HttpNotFound e) {
            return null;
        }
    }

    private CommentResponseDTO mapToDTOWithReplies(Comment comment) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

        List<CommentResponseDTO> allReplies = commentRepository.findByParentCommentIdOrderByCreatedAtAsc(comment.getId())
                .stream()
                .map(this::mapToDTOWithReplies)
                .collect(Collectors.toList());

        return CommentResponseDTO.builder()
                .id(comment.getId())
                .userId(comment.getUsers().getId())
                .username(comment.getUsers().getFirstName() + " " + comment.getUsers().getLastName())
                .profileImage(comment.getUsers().getProfileImage())
                .songId(comment.getSongs().getId())
                .content(comment.getContent())
                .parentId(comment.getParentComment() != null ? comment.getParentComment().getId() : null)
                .createdAt(comment.getCreatedAt() != null ? comment.getCreatedAt().format(formatter) : null)
                .updatedAt(comment.getUpdatedAt() != null ? comment.getUpdatedAt().format(formatter) : null)
                .replies(allReplies)
                .reactionCounts(getReactionCounts(comment.getId()))
                .currentUserReaction(getCurrentUserReaction(comment.getId()))
                .build();
    }

    private CommentResponseDTO mapToDTO(Comment comment) {
        return mapToDTOWithReplies(comment);
    }

    @Override
    public void deleteComment(Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new HttpNotFound("Comment not found"));
        deleteRepliesRecursively(commentId);
        commentRepository.delete(comment);
    }

    private void deleteRepliesRecursively(Long commentId) {
        List<Comment> directReplies = commentRepository.findByParentCommentIdOrderByCreatedAtAsc(commentId);
        for (Comment reply : directReplies) {
            commentRepository.delete(reply);
        }
    }

    @Override
    public CommentResponseDTO updateComment(Long commentId, String content) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new HttpNotFound("Comment not found"));
        comment.setContent(content);
        comment.setUpdatedAt(LocalDateTime.now());
        return mapToDTO(commentRepository.save(comment));
    }
}
