package com.supplog.dto.admin.routine;

import com.supplog.dto.routine.UpdateRoutineRequestDto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class AdminRoutineUpdateRequestDto extends UpdateRoutineRequestDto {

    @NotBlank(message = "{validation.admin.reason.required}")
    @Size(max = 500, message = "{validation.admin.reason.size}")
    private String reason;
}
