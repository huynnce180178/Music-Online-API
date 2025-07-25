package com.ra.base_spring_boot.services.commentReport;

import com.ra.base_spring_boot.dto.req.CommentReportRequestDTO;
import com.ra.base_spring_boot.dto.req.NotificationRequest;
import com.ra.base_spring_boot.dto.resp.CommentReportResponseDTO;
import com.ra.base_spring_boot.model.Comment;
import com.ra.base_spring_boot.model.CommentReport;
import com.ra.base_spring_boot.model.User;
import com.ra.base_spring_boot.model.constants.ReportReasonOption;
import com.ra.base_spring_boot.model.constants.ReportStatus;
import com.ra.base_spring_boot.repository.CommentReportRepository;
import com.ra.base_spring_boot.repository.CommentRepository;
import com.ra.base_spring_boot.repository.IUserRepository;
import com.ra.base_spring_boot.services.comment.CommentService;
import com.ra.base_spring_boot.services.notification.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CommentReportServiceImpl implements CommentReportService {

    @Autowired
    private CommentReportRepository commentReportRepository;
    @Autowired
    private IUserRepository userRepository;
    @Autowired
    private CommentRepository commentRepository;
    @Autowired
    private NotificationService notificationService;
    @Autowired
    private CommentService commentService;

    private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");


    private CommentReportResponseDTO mapToDTO(CommentReport commentReport) {
        return CommentReportResponseDTO.builder()
                .id(commentReport.getId())
                .commentId(commentReport.getComment().getId())
                .reportedById(commentReport.getReportedBy().getId())
                .reportedByName(commentReport.getReportedBy().getFirstName() + " " + commentReport.getReportedBy().getLastName())
                .userReportedId(commentReport.getComment().getUsers().getId())
                .userReportedName(commentReport.getComment().getUsers().getFirstName() + " " + commentReport.getComment().getUsers().getLastName())
                .reason(commentReport.getReason())
                .status(commentReport.getStatus())
                .createdAt(commentReport.getCreatedAt().format(formatter))
                .updatedAt(commentReport.getUpdatedAt().format(formatter))
                .build();
    }

    private CommentReport mapToEntity(CommentReportRequestDTO commentReportRequestDTO) {
        String reasion;
        if (commentReportRequestDTO.getReasonOption() == ReportReasonOption.OTHER) {
            reasion = commentReportRequestDTO.getCustomReason();
        } else {
            reasion = commentReportRequestDTO.getReasonOption().toString();
        }
        return CommentReport.builder()
                .reason(commentReportRequestDTO.getReasonOption().toString())
                .build();
    }


    @Override
    public CommentReportResponseDTO createCommentReport(Long userId, Long commentId, CommentReportRequestDTO commentReportRequestDTO) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Comment not found"));

        boolean exists = commentReportRepository.findByReportedByAndComment(user, comment).isPresent();
        if (exists) {
            throw new RuntimeException("You already reported this comment.");
        }
        CommentReport commentReport = mapToEntity(commentReportRequestDTO);
        commentReport.setReportedBy(user);
        commentReport.setComment(comment);
        commentReport.setStatus(ReportStatus.PENDING);
        commentReport.setCreatedAt(LocalDateTime.now());
        commentReport.setUpdatedAt(LocalDateTime.now());
        commentReportRepository.save(commentReport);

        return mapToDTO(commentReport);
    }

    @Override
    public CommentReportResponseDTO updateCommentReport(Long userId, Long commentId, CommentReportRequestDTO commentReportRequestDTO) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("Reported by user not found with id: " + userId));
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Comment not found with id: " + commentId));
        CommentReport commentReport = commentReportRepository
                .findByReportedByAndComment(user, comment).orElseThrow(() -> new RuntimeException("Reported by user not found with is " + userId + "and commentId is " + commentId));
        commentReport.setReason(mapToEntity(commentReportRequestDTO).getReason());
        commentReport.setUpdatedAt(LocalDateTime.now());
        commentReportRepository.save(commentReport);

        return mapToDTO(commentReport);
    }

    @Override
    public Page<CommentReportResponseDTO> getCommentReport(Pageable pageable) {
        Page<CommentReport> commentReports = commentReportRepository.findAll(pageable);
        List<CommentReportResponseDTO> listDTO = commentReports.getContent().stream().map(this::mapToDTO).collect(Collectors.toList());
        return new PageImpl<>(listDTO, pageable, commentReports.getTotalElements());
    }

    @Override
    public CommentReportResponseDTO getCommentReportById(Long id) {
        CommentReport commentReport = commentReportRepository.findById(id).orElse(null);
        if (commentReport == null) {
            throw new RuntimeException("Comment report not found");
        }
        return mapToDTO(commentReport);
    }

    @Override
    public void updateStatusCommentReport(Long id, ReportStatus status) {
        CommentReport commentReport = commentReportRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Comment report not found"));

        commentReport.setStatus(status);
        commentReport.setUpdatedAt(LocalDateTime.now());
        commentReportRepository.save(commentReport);

        Comment comment = commentReport.getComment();
        User reportedBy = commentReport.getReportedBy();
        User commentOwner = comment.getUsers();
        String songTitle = comment.getSongs() != null ? comment.getSongs().getTitle() : "Unknown";
        String reason = commentReport.getReason();

        if (status == ReportStatus.RESOLVED) {
            String ownerMessage = String.format(
                "Your comment on the song '%s' was reported for reason: '%s'.",
                songTitle, reason
            );
            notificationService.createNotification(
                new NotificationRequest(
                    commentOwner.getId(),
                    ownerMessage,
                    "comment_reported"
                )
            );
            // Notify reporter
            String reporterMessage = String.format(
                "The comment you reported on the song '%s' has been approved by the admin. Reason: '%s'.",
                songTitle, reason
            );
            notificationService.createNotification(
                new NotificationRequest(
                    reportedBy.getId(),
                    reporterMessage,
                    "comment_report_resolved"
                )
            );
        } else if (status == ReportStatus.REJECTED) {
            String reporterMessage = String.format(
                "Your report for the comment on the song '%s' was rejected by the admin. Reason: '%s'.",
                songTitle, reason
            );
            notificationService.createNotification(
                new NotificationRequest(
                    reportedBy.getId(),
                    reporterMessage,
                    "comment_report_rejected"
                )
            );
        }
        if(status == ReportStatus.RESOLVED){
            commentReportRepository.deleteById(id);
            commentService.deleteComment(comment.getId());
        }
    }

    @Override
    public List<CommentReportResponseDTO> getMyCommentReport(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        List<CommentReport> commentReports = commentReportRepository.findByReportedBy(user);
        List<CommentReportResponseDTO> listDTO = commentReports.stream().map(this::mapToDTO).collect(Collectors.toList());
        return listDTO;
    }

    @Override
    public List<CommentReportResponseDTO> getReportsByStatus(ReportStatus reportStatus) {
        List<CommentReport> commentReportList = commentReportRepository.findByStatus(reportStatus);
        return commentReportList.stream().map(this::mapToDTO).collect(Collectors.toList());
    }

}
