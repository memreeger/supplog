package com.supplog.service.admin.adminSupportService;

import com.supplog.dto.admin.support.AdminSupportResponseDto;
import com.supplog.enums.SupportStatus;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AdminSupportService {
    Page<AdminSupportResponseDto> search(SupportStatus status, Long userId, Pageable pageable);
    void revoke(Long adminId, Long relationshipId, String reason);
}
