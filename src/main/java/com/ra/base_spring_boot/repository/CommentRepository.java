package com.ra.base_spring_boot.repository;

import com.ra.base_spring_boot.model.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    @Query("""
    select count(c) from Comment c
    where c.createdAt > :start
    and c.createdAt < :end
    """)
    Long countCommentByDate(LocalDateTime start, LocalDateTime end);

    List<Comment> findBySongsIdOrderByCreatedAtDesc(Long songId);

    List<Comment> findByParentCommentIdOrderByCreatedAtAsc(Long parentId);

    List<Comment> findBySongsIdAndParentCommentIsNullOrderByCreatedAtDesc(Long songId);
}
