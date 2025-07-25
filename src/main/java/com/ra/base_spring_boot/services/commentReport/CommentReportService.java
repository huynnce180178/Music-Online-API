package com.ra.base_spring_boot.services.commentReport;

import com.ra.base_spring_boot.dto.req.CommentReportRequestDTO;
import com.ra.base_spring_boot.dto.resp.CommentReportResponseDTO;
import com.ra.base_spring_boot.model.CommentReport;
import com.ra.base_spring_boot.model.constants.ReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CommentReportService {
    CommentReportResponseDTO createCommentReport(Long userId, Long commentId, CommentReportRequestDTO commentReportRequestDTO);
    CommentReportResponseDTO updateCommentReport(Long userId, Long commentId, CommentReportRequestDTO commentReportRequestDTO);
    Page<CommentReportResponseDTO> getCommentReport(Pageable pageable);
    List<CommentReportResponseDTO> getReportsByStatus(ReportStatus reportStatus);
    CommentReportResponseDTO getCommentReportById(Long id);
    void updateStatusCommentReport(Long id, ReportStatus status);
    List<CommentReportResponseDTO> getMyCommentReport(Long userId);
}
