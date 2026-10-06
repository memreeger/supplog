package com.supplog.dto.admin.user;

import com.supplog.dto.user.UserResponseDto;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class AdminUserResponseDto extends UserResponseDto {
    private boolean deleted;

    private LocalDateTime updatedAt;
}
