package com.ra.base_spring_boot.dto.resp;

import com.ra.base_spring_boot.model.constants.ReportStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class CommentReportResponseDTO {
    private Long id;
    private Long commentId;

    private Long reportedById;
    private String reportedByName;

    private Long userReportedId;
    private String userReportedName;

    private String reason;
    private ReportStatus status;
    private String createdAt;
    private String updatedAt;

}
