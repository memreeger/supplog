package com.supplog.controller;

import com.supplog.dto.routineExecution.RoutineExecutionResponseDto;
import com.supplog.service.routineExecution.RoutineExecutionService;
import com.supplog.service.user.impl.CustomUserDetails;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/routine-executions")
@RequiredArgsConstructor
@Validated
public class RoutineExecutionController {

    private final RoutineExecutionService
            routineExecutionService;

    @GetMapping("/today")
    public ResponseEntity<List<RoutineExecutionResponseDto>> getToday(
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return ResponseEntity.ok(
                routineExecutionService.getToday(
                        currentUser.getId()
                )
        );
    }

    @GetMapping("/history")
    public ResponseEntity<Page<RoutineExecutionResponseDto>> getHistory(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate dateFrom,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate dateTo,
            @RequestParam(defaultValue = "0")
            @PositiveOrZero(message = "{validation.page.number.invalid}")
            int page,
            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "{validation.page.size.invalid}")
            @Max(value = 100, message = "{validation.page.size.invalid}")
            int size
    ) {
        return ResponseEntity.ok(
                routineExecutionService.getHistory(
                        currentUser.getId(),
                        dateFrom,
                        dateTo,
                        PageRequest.of(page, size)
                )
        );
    }

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
