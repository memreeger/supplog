package com.supplog.dto.admin.user;

import com.supplog.enums.RoleName;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UpdateUserRoleDto {
    @NotNull(message = "{validation.role.required}")
    private RoleName roleName;
}
