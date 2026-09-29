package com.supplog.dto.admin.user;

import com.supplog.enums.RoleName;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UpdateUserRoleRequestDto {
    @NotNull(message = "{validation.role.required}")
    private RoleName roleName;

    @NotBlank(message = "{validation.admin.reason.required}")
    @Size(max = 500, message = "{validation.admin.reason.size}")
    private String reason;
}
