package com.supplog.controller.admin;

import com.supplog.dto.admin.AdminReasonRequestDto;
import com.supplog.dto.admin.support.AdminSupportResponseDto;
import com.supplog.enums.SupportStatus;
import com.supplog.service.admin.adminSupportService.AdminSupportService;
import com.supplog.service.user.impl.CustomUserDetails;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/support-relationships")
@PreAuthorize("hasRole('ADMIN')")
public class AdminSupportController {

    private final AdminSupportService service;

    public AdminSupportController(AdminSupportService service) {
        this.service = service;
    }

    @GetMapping
    public Page<AdminSupportResponseDto> search(
            @RequestParam(required = false) SupportStatus status,
            @RequestParam(required = false) Long userId,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return service.search(status, userId, pageable);
    }

    @PatchMapping("/{id}/revoke")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revoke(
            @AuthenticationPrincipal CustomUserDetails admin,
            @PathVariable @Positive Long id,
            @Valid @RequestBody AdminReasonRequestDto request
    ) {
        service.revoke(admin.getId(), id, request.reason());
    }
}
