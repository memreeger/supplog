package com.supplog.dto.supplement;

import com.supplog.enums.RoutineCategory;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class CreateSupplementRequestDto extends BaseSupplementDto {
    @NotBlank(message = "{validation.supplement.dosage.required}")
    private String suppDosage;

    @NotNull(message = "{validation.supplement.expireDate.required}")
    @FutureOrPresent(message = "{supplement.expire.date.invalid}")
    private LocalDate expireDate;

    @NotNull(message = "{validation.supplement.type.required}")
    private RoutineCategory type;

    @NotNull(message = "{validation.userId.required}")
    private Long userId;
}
