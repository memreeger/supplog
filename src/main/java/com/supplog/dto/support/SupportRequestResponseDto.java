package com.supplog.dto.support;

import com.supplog.enums.SupportStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class SupportRequestResponseDto {

    private Long id;

    private SupportUserSummaryDto supportedUser;

    private SupportUserSummaryDto supporter;

    private SupportStatus status;

    private LocalDateTime createdAt;

    private LocalDateTime requestedAt;

    private LocalDateTime respondedAt;

}