package com.ra.base_spring_boot.dto.req;

import com.ra.base_spring_boot.model.constants.ReportReasonOption;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class CommentReportRequestDTO {

    @NotNull
    private ReportReasonOption reasonOption;

    private String customReason; // optional if reasonOption == OTHER
}
