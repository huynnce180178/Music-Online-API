package com.ra.base_spring_boot.repository;

import com.ra.base_spring_boot.model.Comment;
import com.ra.base_spring_boot.model.CommentReport;
import com.ra.base_spring_boot.model.User;
import com.ra.base_spring_boot.model.constants.ReportStatus;
import jdk.dynalink.Operation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CommentReportRepository extends JpaRepository<CommentReport, Long> {
    List<CommentReport> findByStatus(ReportStatus status);

    Optional<CommentReport> findByReportedByAndComment(User reportedBy, Comment comment);

    List<CommentReport> findByReportedBy(User reportedBy);
}
