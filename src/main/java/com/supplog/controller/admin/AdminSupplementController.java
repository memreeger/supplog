package com.supplog.controller.admin;

import com.supplog.dto.admin.supplement.AdminSupplementResponseDto;
import com.supplog.dto.admin.supplement.UpdateSupplementRequestDtoAdmin;
import com.supplog.service.admin.adminSupplementService.AdminSupplementService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
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
    public List<AdminSupplementResponseDto> findAll() {
        return adminSupplementService.getAll();
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
            @PathVariable
            @Positive(message = "{validation.id.positive}") Long id
    ) {
        adminSupplementService.activateSupplementById(id);
    }

    @PatchMapping("/{id}/deactivate")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivateSupplementById(
            @PathVariable
            @Positive(message = "{validation.id.positive}") Long id
    ) {
        adminSupplementService.deactivateSupplementById(id);
    }

    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateSupplementById(
            @PathVariable
            @Positive(message = "{validation.id.positive}") Long id,
            @Valid @RequestBody UpdateSupplementRequestDtoAdmin requestDto
    ) {
        adminSupplementService.updateSupplementById(id, requestDto);
    }
}

