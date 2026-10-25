package com.supplog.dto.notification;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class NudgeMuteResponseDto {

    private Long senderUserId;

    private String firstName;

    private String username;
}
