package com.supplog.dto.notification;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;

@Getter
@AllArgsConstructor
public class NudgeResponseDto {

    private Long id;

    private Instant createdAt;
}
