package com.supplog.controller;

import com.supplog.dto.support.*;
import com.supplog.service.support.SupportService;
import com.supplog.service.user.impl.CustomUserDetails;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/support")
@RequiredArgsConstructor
@Validated
public class SupportController {

    private final SupportService supportService;

    @PostMapping("/requests")
    public ResponseEntity<SupportRequestResponseDto> sendRequest(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @Valid @RequestBody SupportRequestCreateDto request
    ) {

        SupportRequestResponseDto response =
                supportService.sendRequest(
                        currentUser.getId(),
                        request
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping("/requests/incoming")
    public ResponseEntity<List<SupportRequestResponseDto>>
    getIncomingRequests(
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {

        return ResponseEntity.ok(
                supportService.getIncomingRequests(
                        currentUser.getId()
                )
        );
    }

    @GetMapping("/requests/outgoing")
    public ResponseEntity<List<SupportRequestResponseDto>>
    getOutgoingRequests(
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {

        return ResponseEntity.ok(
                supportService.getOutgoingRequests(
                        currentUser.getId()
                )
        );
    }

    @PatchMapping("/requests/{relationshipId}/accept")
    public ResponseEntity<SupportConnectionResponseDto>
    acceptRequest(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable
            @Positive(message = "{validation.id.positive}")
            Long relationshipId
    ) {

        return ResponseEntity.ok(
                supportService.acceptRequest(
                        currentUser.getId(),
                        relationshipId
                )
        );
    }

    @PatchMapping("/requests/{relationshipId}/reject")
    public ResponseEntity<SupportRequestResponseDto>
    rejectRequest(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable
            @Positive(message = "{validation.id.positive}")
            Long relationshipId
    ) {

        return ResponseEntity.ok(
                supportService.rejectRequest(
                        currentUser.getId(),
                        relationshipId
                )
        );
    }

    @PatchMapping("/requests/{relationshipId}/cancel")
    public ResponseEntity<SupportRequestResponseDto>
    cancelRequest(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable
            @Positive(message = "{validation.id.positive}")
            Long relationshipId
    ) {

        return ResponseEntity.ok(
                supportService.cancelRequest(
                        currentUser.getId(),
                        relationshipId
                )
        );
    }

    @GetMapping("/connections/supporters")
    public ResponseEntity<List<SupportConnectionResponseDto>>
    getMySupporters(
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {

        return ResponseEntity.ok(
                supportService.getMySupporters(
                        currentUser.getId()
                )
        );
    }

    @GetMapping("/connections/supported-users")
    public ResponseEntity<List<SupportConnectionResponseDto>>
    getSupportedUsers(
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {

        return ResponseEntity.ok(
                supportService.getSupportedUsers(
                        currentUser.getId()
                )
        );
    }

    @DeleteMapping("/connections/{relationshipId}")
    public ResponseEntity<Void> revokeRelationship(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable
            @Positive(message = "{validation.id.positive}")
            Long relationshipId
    ) {

        supportService.revokeRelationship(
                currentUser.getId(),
                relationshipId
        );

        return ResponseEntity.noContent().build();
    }

    @PatchMapping(
            "/connections/{relationshipId}/access-scope"
    )
    public ResponseEntity<SupportConnectionResponseDto>
    updateAccessScope(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable
            @Positive(message = "{validation.id.positive}")
            Long relationshipId,
            @Valid @RequestBody SupportAccessUpdateDto request
    ) {

        return ResponseEntity.ok(
                supportService.updateAccessScope(
                        currentUser.getId(),
                        relationshipId,
                        request
                )
        );
    }

    @PutMapping(
            "/connections/{relationshipId}/routines"
    )
    public ResponseEntity<SupportConnectionResponseDto>
    updateSelectedRoutines(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable
            @Positive(message = "{validation.id.positive}")
            Long relationshipId,
            @Valid
            @RequestBody
            SupportRoutineSelectionUpdateDto request
    ) {

        return ResponseEntity.ok(
                supportService.updateSelectedRoutines(
                        currentUser.getId(),
                        relationshipId,
                        request
                )
        );
    }

    @GetMapping(
            "/connections/{relationshipId}/routines"
    )
    public ResponseEntity<List<SupportRoutineResponseDto>>
    getSharedRoutines(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable
            @Positive(message = "{validation.id.positive}")
            Long relationshipId
    ) {

        return ResponseEntity.ok(
                supportService.getSharedRoutines(
                        currentUser.getId(),
                        relationshipId
                )
        );
    }

    @GetMapping(
            "/connections/{relationshipId}/routine-selection"
    )
    public ResponseEntity<SupportRoutineSelectionResponseDto>
    getRoutineSelection(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable
            @Positive(message = "{validation.id.positive}")
            Long relationshipId
    ) {

        return ResponseEntity.ok(
                supportService.getRoutineSelection(
                        currentUser.getId(),
                        relationshipId
                )
        );
    }

}