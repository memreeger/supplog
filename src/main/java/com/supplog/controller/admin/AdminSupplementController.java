package com.supplog.controller.admin;

import com.supplog.dto.supplement.SupplementResponseDto;
import com.supplog.dto.supplement.UpdateSupplementRequestDto;
import com.supplog.service.admin.adminSupplementService.AdminSupplementService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
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
    public List<SupplementResponseDto> findAll() {
        return adminSupplementService.getAll();
    }

    @GetMapping("/{id}")
    public SupplementResponseDto getById(
            @PathVariable Long id
    ) {
        return adminSupplementService.getById(id);
    }

    @GetMapping("/active")
    public List<SupplementResponseDto> getActiveSupplements() {
        return adminSupplementService.getAllActiveSupplements();
    }

    @GetMapping("/inactive")
    public List<SupplementResponseDto> getInactiveSupplements() {
        return adminSupplementService.getAllInactiveSupplements();
    }

    @GetMapping("/users/{userId}")
    public List<SupplementResponseDto> getSupplementsByUserId(
            @PathVariable Long userId
    ) {
        return adminSupplementService
                .getAllSupplementsByUserId(userId);
    }

    @PatchMapping("/{id}/activate")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void activateSupplementById(
            @PathVariable Long id
    ) {
        adminSupplementService.activateSupplementById(id);
    }

    @PatchMapping("/{id}/deactivate")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivateSupplementById(
            @PathVariable Long id
    ) {
        adminSupplementService.deactivateSupplementById(id);
    }

    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateSupplementById(
            @PathVariable Long id,
            @Valid @RequestBody UpdateSupplementRequestDto requestDto
    ) {
        adminSupplementService.updateSupplementById(id, requestDto);
    }
}

