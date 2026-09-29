package com.supplog.controller.admin;

import com.supplog.dto.admin.audit.AdminAuditResponseDto;
import com.supplog.service.admin.audit.AdminAuditService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/audits")
@PreAuthorize("hasRole('ADMIN')")
public class AdminAuditController {

    private final AdminAuditService adminAuditService;

    public AdminAuditController(AdminAuditService adminAuditService) {
        this.adminAuditService = adminAuditService;
    }

    @GetMapping
    public List<AdminAuditResponseDto> getRecentAudits() {
        return adminAuditService.getRecent();
    }
}
