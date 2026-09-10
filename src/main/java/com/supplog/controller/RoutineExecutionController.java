package com.supplog.controller;

import com.supplog.dto.routineExecution.RoutineExecutionResponseDto;
import com.supplog.service.routineExecution.RoutineExecutionService;
import com.supplog.service.user.impl.CustomUserDetails;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/routine-executions")
@RequiredArgsConstructor
public class RoutineExecutionController {

    private final RoutineExecutionService
            routineExecutionService;

    @PatchMapping(
            "/routines/{routineId}/today/complete"
    )
    public ResponseEntity<RoutineExecutionResponseDto>
    completeToday(
            @AuthenticationPrincipal
            CustomUserDetails currentUser,

            @PathVariable
            @Positive(message = "{validation.id.positive}")
            Long routineId
    ) {

        return ResponseEntity.ok(
                routineExecutionService
                        .completeToday(
                                currentUser.getId(),
                                routineId
                        )
        );
    }

    @PatchMapping(
            "/routines/{routineId}/today/skip"
    )
    public ResponseEntity<RoutineExecutionResponseDto>
    skipToday(
            @AuthenticationPrincipal
            CustomUserDetails currentUser,

            @PathVariable
            @Positive(message = "{validation.id.positive}")
            Long routineId
    ) {

        return ResponseEntity.ok(
                routineExecutionService
                        .skipToday(
                                currentUser.getId(),
                                routineId
                        )
        );
    }
}