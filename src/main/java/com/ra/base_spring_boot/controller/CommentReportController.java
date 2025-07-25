package com.ra.base_spring_boot.controller;

import com.ra.base_spring_boot.dto.ResponseWrapper;
import com.ra.base_spring_boot.dto.req.CommentReportRequestDTO;
import com.ra.base_spring_boot.model.constants.ReportStatus;
import com.ra.base_spring_boot.security.principle.MyUserDetails;
import com.ra.base_spring_boot.services.commentReport.CommentReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Comment Report Controller")
@RestController
@RequestMapping("/api/v1/")
public class CommentReportController {
    @Autowired
    private CommentReportService commentReportService;

    @Operation(summary = "Gửi báo cáo bình luận", description = "Người dùng gửi báo cáo cho một bình luận cụ thể")
    @PostMapping("/user/commentReport/add/{commentId}")
    public ResponseEntity<?> createCommentReport(@AuthenticationPrincipal MyUserDetails userDetails,
                                                 @PathVariable Long commentId,
                                                 @RequestBody CommentReportRequestDTO commentReportRequestDTO) {
        Long userId = userDetails.getUser().getId();
        return ResponseEntity.ok(ResponseWrapper.builder()
                .data(commentReportService.createCommentReport(userId, commentId, commentReportRequestDTO))
                .code(HttpStatus.OK.value())
                .status(HttpStatus.OK)
                .build());
    }

    @Operation(summary = "Cập nhật báo cáo bình luận", description = "Người dùng cập nhật nội dung lý do báo cáo bình luận đã gửi")
    @PostMapping("/user/commentReport/update/{commentId}")
    public ResponseEntity<?> updateCommentReport(@AuthenticationPrincipal MyUserDetails userDetails,
                                                 @PathVariable Long commentId,
                                                 @RequestBody CommentReportRequestDTO commentReportRequestDTO) {
        Long userId = userDetails.getUser().getId();
        return ResponseEntity.ok(ResponseWrapper.builder()
                .data(commentReportService.updateCommentReport(userId, commentId, commentReportRequestDTO))
                .code(HttpStatus.OK.value())
                .status(HttpStatus.OK)
                .build());
    }

    @Operation(summary = "Lấy tất cả báo cáo (Admin)", description = "Admin lấy danh sách tất cả các báo cáo với phân trang và sắp xếp")
    @GetMapping("/admin/commentReport/getAll")
    public ResponseEntity<?> getAllCommentReport(@RequestParam(name = "page", defaultValue = "0") int page,
                                                 @RequestParam(name = "limit", defaultValue = "10") int limit,
                                                 @RequestParam(name = "sortBy", defaultValue = "id") String sortBy,
                                                 @RequestParam(name = "orderBy", defaultValue = "asc") String orderBy) {
        Sort sort = orderBy.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, limit, sort);
        return ResponseEntity.ok(ResponseWrapper.builder()
                .data(commentReportService.getCommentReport(pageable))
                .code(HttpStatus.OK.value())
                .status(HttpStatus.OK)
                .build());
    }
    @Operation(summary = "Lọc báo cáo theo trạng thái (Admin)", description = "Admin lọc danh sách báo cáo theo trạng thái (PENDING, RESOLVED, ...)")
    @GetMapping("/admin/commentReport/getAllByStatus")
    public ResponseEntity<?> getAllCommentByStatus(@RequestParam ReportStatus status){
        return ResponseEntity.ok(ResponseWrapper.builder()
                .data(commentReportService.getReportsByStatus(status))
                .code(HttpStatus.OK.value())
                .status(HttpStatus.OK)
                .build());
    }

    @Operation(summary = "Xem chi tiết báo cáo", description = "Người dùng xem chi tiết báo cáo của mình theo ID")
    @GetMapping("/user/commentReport/getCommentReportById/{id}")
    public ResponseEntity<?> getCommentReportByID(@PathVariable Long id) {
        return ResponseEntity.ok(ResponseWrapper.builder()
                .data(commentReportService.getCommentReportById(id))
                .code(HttpStatus.OK.value())
                .status(HttpStatus.OK)
                .build());
    }

    @Operation(summary = "Cập nhật trạng thái báo cáo (Admin)", description = "Admin cập nhật trạng thái của báo cáo (VD: chuyển sang RESOLVED)")
    @PatchMapping("/admin/commentReport/updateCommentReport/{id}")
    public ResponseEntity<?> updateCommentReport(@PathVariable Long id, @RequestParam ReportStatus status) {
        commentReportService.updateStatusCommentReport(id, status);
        return ResponseEntity.ok(ResponseWrapper.builder()
                .data("Report status updated successfully.")
                .code(HttpStatus.OK.value())
                .status(HttpStatus.OK)
                .build());
    }

    @Operation(summary = "Xem danh sách báo cáo của tôi", description = "Người dùng xem tất cả các báo cáo mà mình đã gửi")
    @GetMapping("/user/commentReport/getMyCommentReport")
    public ResponseEntity<?> getCommentReportByID(@AuthenticationPrincipal MyUserDetails userDetails) {
        Long userId = userDetails.getUser().getId();
        return ResponseEntity.ok(ResponseWrapper.builder()
                .data(commentReportService.getMyCommentReport(userId))
                .code(HttpStatus.OK.value())
                .status(HttpStatus.OK)
                .build());
    }


}
