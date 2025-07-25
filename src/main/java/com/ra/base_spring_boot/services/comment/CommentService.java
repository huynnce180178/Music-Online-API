package com.ra.base_spring_boot.services.comment;

import com.ra.base_spring_boot.dto.req.CommentRequestDTO;
import com.ra.base_spring_boot.dto.resp.CommentResponseDTO;

import java.time.LocalDateTime;
import java.util.List;

public interface CommentService {
    Long countComment(LocalDateTime start, LocalDateTime end);
    CommentResponseDTO createComment(CommentRequestDTO commentRequestDTO);
    List<CommentResponseDTO> getCommentsBySongId(Long songId);
    void deleteComment(Long commentId);
    CommentResponseDTO updateComment(Long commentId, String content);
}
