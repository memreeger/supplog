package com.supplog.controller;

import com.supplog.dto.supplement.CreateSupplementRequestDto;
import com.supplog.dto.supplement.SupplementResponseDto;
import com.supplog.dto.supplement.UpdateSupplementDosageRequestDto;
import com.supplog.dto.supplement.UpdateSupplementRequestDto;
import com.supplog.service.supplement.SupplementService;
import com.supplog.service.user.impl.CustomUserDetails;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/supplements")
public class SupplementController {

    private final SupplementService supplementService;

    public SupplementController(
            SupplementService supplementService
    ) {
        this.supplementService = supplementService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void addSupplement(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @Valid @RequestBody CreateSupplementRequestDto requestDto
    ) {
        supplementService.addSupplement(
                currentUser.getId(),
                requestDto
        );
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<SupplementResponseDto> getMySupplements(
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return supplementService.getMySupplements(
                currentUser.getId()
        );
    }

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public SupplementResponseDto getMySupplementById(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable Long id
    ) {
        return supplementService.getMySupplementById(
                currentUser.getId(),
                id
        );
    }

    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateSupplement(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable Long id,
            @Valid @RequestBody UpdateSupplementRequestDto requestDto
    ) {
        supplementService.updateMySupplement(
                currentUser.getId(),
                id,
                requestDto
        );
    }

    @PatchMapping("/{id}/dosage")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateSupplementDosage(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable Long id,
            @Valid @RequestBody UpdateSupplementDosageRequestDto requestDto
    ) {
        supplementService.updateMySupplementDosage(
                currentUser.getId(),
                id,
                requestDto
        );
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteSupplement(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable Long id
    ) {
        supplementService.deleteMySupplement(
                currentUser.getId(),
                id
        );
    }
}