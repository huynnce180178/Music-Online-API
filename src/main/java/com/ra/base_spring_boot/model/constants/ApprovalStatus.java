package com.ra.base_spring_boot.model.constants;

import lombok.Getter;

@Getter
public enum ApprovalStatus {
    PENDING,        // Đang chờ phê duyệt
    APPROVED,       // Đã được phê duyệt
    REJECTED,       // Bị từ chối do không đạt yêu cầu
    DELETED_BY_ADMIN, // Bị admin xóa do vi phạm
    DELETED_BY_ARTIST, // Bị nghệ sĩ xóa
    EXPIRED; // Quảng cáo quá hạn
}
