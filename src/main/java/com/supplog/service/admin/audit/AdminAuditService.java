package com.supplog.service.admin.audit;

import com.supplog.dto.admin.audit.AdminAuditResponseDto;
import java.util.List;

public interface AdminAuditService {
    void record(Long adminUserId, String action, String resourceType, Long resourceId,
            String oldValue, String newValue, String reason,
            boolean successful, String errorMessage);
    List<AdminAuditResponseDto> getRecent();
    List<AdminAuditResponseDto> getRecentForResource(String resourceType, Long resourceId);
}
