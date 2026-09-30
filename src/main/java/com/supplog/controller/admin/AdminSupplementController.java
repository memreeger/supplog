package com.supplog.controller.admin;

import com.supplog.dto.admin.supplement.AdminSupplementResponseDto;
import com.supplog.dto.admin.supplement.AdminSupplementDetailResponseDto;
import com.supplog.dto.admin.supplement.UpdateSupplementRequestDtoAdmin;
import com.supplog.dto.admin.AdminReasonRequestDto;
import com.supplog.enums.RoutineCategory;
import com.supplog.service.admin.adminSupplementService.AdminSupplementService;
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
@RequestMapping("/api/v1/admin/supplements")
@PreAuthorize("hasRole('ADMIN')")
public class AdminSupplementController {

    private final AdminSupplementService adminSupplementService;

    public AdminSupplementController(
            AdminSupplementService adminSupplementService
    ) {
        this.adminSupplementService = adminSupplementService;
    }

    @GetMapping
    public Page<AdminSupplementResponseDto> findAll(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) RoutineCategory type,
            @RequestParam(required = false) Boolean active,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return adminSupplementService.search(userId, name, type, active, pageable);
    }

    @GetMapping("/{id}/detail")
    public AdminSupplementDetailResponseDto getDetail(
            @PathVariable @Positive(message = "{validation.id.positive}") Long id
    ) {
        return adminSupplementService.getDetail(id);
    }

    @GetMapping("/{id}")
    public AdminSupplementResponseDto getById(
            @PathVariable
            @Positive(message = "{validation.id.positive}") Long id
    ) {
        return adminSupplementService.getById(id);
    }

    @GetMapping("/active")
    public List<AdminSupplementResponseDto> getActiveSupplements() {
        return adminSupplementService.getAllActiveSupplements();
    }

    @GetMapping("/inactive")
    public List<AdminSupplementResponseDto> getInactiveSupplements() {
        return adminSupplementService.getAllInactiveSupplements();
    }

    @GetMapping("/users/{userId}")
    public List<AdminSupplementResponseDto> getSupplementsByUserId(
            @PathVariable
            @Positive(message = "{validation.id.positive}") Long userId
    ) {
        return adminSupplementService
                .getAllSupplementsByUserId(userId);
    }

    @PatchMapping("/{id}/activate")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void activateSupplementById(
            @AuthenticationPrincipal CustomUserDetails admin,
            @PathVariable
            @Positive(message = "{validation.id.positive}") Long id,
            @Valid @RequestBody AdminReasonRequestDto request
    ) {
        adminSupplementService.activateSupplementById(admin.getId(), id, request.reason());
    }

    @PatchMapping("/{id}/deactivate")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivateSupplementById(
            @AuthenticationPrincipal CustomUserDetails admin,
            @PathVariable
            @Positive(message = "{validation.id.positive}") Long id,
            @Valid @RequestBody AdminReasonRequestDto request
    ) {
        adminSupplementService.deactivateSupplementById(admin.getId(), id, request.reason());
    }

    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateSupplementById(
            @AuthenticationPrincipal CustomUserDetails admin,
            @PathVariable
            @Positive(message = "{validation.id.positive}") Long id,
            @Valid @RequestBody UpdateSupplementRequestDtoAdmin requestDto
    ) {
        adminSupplementService.updateSupplementById(admin.getId(), id, requestDto);
    }
}

