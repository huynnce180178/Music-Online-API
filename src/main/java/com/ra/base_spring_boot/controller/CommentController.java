package com.ra.base_spring_boot.controller;

import com.ra.base_spring_boot.dto.req.CommentRequestDTO;
import com.ra.base_spring_boot.dto.resp.CommentResponseDTO;
import com.ra.base_spring_boot.services.comment.CommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Comment Controller")
@RestController
@RequestMapping("/api/v1/comments")
public class CommentController {
    @Autowired
    private CommentService commentService;

    @Operation(summary = "Create a new comment")
    @PostMapping
    public ResponseEntity<CommentResponseDTO> createComment(@RequestBody CommentRequestDTO commentRequestDTO) {
        return new ResponseEntity<>(commentService.createComment(commentRequestDTO), HttpStatus.CREATED);
    }

    @Operation(summary = "Get all comments for a song")
    @GetMapping("/song/{songId}")
    public ResponseEntity<List<CommentResponseDTO>> getCommentsBySongId(@PathVariable Long songId) {
        return ResponseEntity.ok(commentService.getCommentsBySongId(songId));
    }

    @Operation(summary = "Update a comment")
    @PutMapping("/{commentId}")
    public ResponseEntity<CommentResponseDTO> updateComment(
            @PathVariable Long commentId,
            @RequestParam String content) {
        return ResponseEntity.ok(commentService.updateComment(commentId, content));
    }

    @Operation(summary = "Delete a comment")
    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> deleteComment(@PathVariable Long commentId) {
        commentService.deleteComment(commentId);
        return ResponseEntity.noContent().build();
    }
}
