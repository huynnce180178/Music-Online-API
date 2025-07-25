package com.ra.base_spring_boot.controller;

import com.ra.base_spring_boot.dto.ResponseWrapper;
import com.ra.base_spring_boot.dto.resp.ReactionCountDTO;
import com.ra.base_spring_boot.model.constants.ReactionType;
import com.ra.base_spring_boot.services.reaction.ReactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Reaction Controller")
@RestController
@RequestMapping("/api/v1/reactions")
public class ReactionController {
    @Autowired
    private ReactionService reactionService;

    @Operation(summary = "Chuyển đổi phản ứng trên bình luận")
    @PostMapping("/{commentId}")
    public ResponseEntity<ResponseWrapper<?>> toggleReaction(
            @PathVariable Long commentId,
            @RequestParam ReactionType type) {
        reactionService.toggleReaction(commentId, type);
        List<ReactionCountDTO> reactionCounts = reactionService.getReactionCounts(commentId);
        return ResponseEntity.ok(
                ResponseWrapper.builder()
                        .data(reactionCounts)
                        .code(HttpStatus.OK.value())
                        .status(HttpStatus.OK)
                        .build()
        );
    }

    @Operation(summary = "Xóa phản ứng khỏi bình luận")
    @DeleteMapping("/{commentId}")
    public ResponseEntity<ResponseWrapper<?>> removeReaction(@PathVariable Long commentId) {
        reactionService.removeReaction(commentId);
        List<ReactionCountDTO> reactionCounts = reactionService.getReactionCounts(commentId);
        return ResponseEntity.ok(
                ResponseWrapper.builder()
                        .data(reactionCounts)
                        .code(HttpStatus.OK.value())
                        .status(HttpStatus.OK)
                        .build()
        );
    }

    @Operation(summary = "Lấy số lượt phản ứng cho một bình luận")
    @GetMapping("/{commentId}/counts")
    public ResponseEntity<ResponseWrapper<?>> getReactionCounts(@PathVariable Long commentId) {
        List<ReactionCountDTO> reactionCounts = reactionService.getReactionCounts(commentId);
        return ResponseEntity.ok(
                ResponseWrapper.builder()
                        .data(reactionCounts)
                        .code(HttpStatus.OK.value())
                        .status(HttpStatus.OK)
                        .build()
        );
    }
}