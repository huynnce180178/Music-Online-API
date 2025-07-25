package com.ra.base_spring_boot.repository;

import com.ra.base_spring_boot.model.Reaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReactionRepository extends JpaRepository<Reaction, Long> {
    List<Reaction> findByUserIdAndCommentId(Long userId, Long commentId);

    @Query("SELECT r.type, COUNT(r) FROM Reaction r WHERE r.comment.id = :commentId GROUP BY r.type")
    List<Object[]> countReactionsByCommentId(Long commentId);
}
