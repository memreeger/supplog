package com.supplog.dto.admin.user;

import com.supplog.dto.user.UserResponseDto;
import com.supplog.enums.RoleName;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Set;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class AdminUserResponseDto extends UserResponseDto {
    private boolean deleted;

    private Set<RoleName> roles;

    private LocalDateTime updatedAt;
}
