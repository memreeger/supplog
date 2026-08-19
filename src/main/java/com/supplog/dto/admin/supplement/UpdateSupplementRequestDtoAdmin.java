package com.supplog.dto.admin.supplement;

import com.supplog.enums.RoutineCategory;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UpdateSupplementRequestDtoAdmin {
    @NotBlank(message = "{validation.supplement.name.required}")
    @Size(min = 2, max = 100, message = "{validation.supplement.name.size}")
    private String name;

    @NotBlank(message = "{validation.supplement.dosage.required}")
    @Size(max = 100, message = "{validation.supplement.dosage.size}")
    private String suppDosage;

    @NotNull(message = "{validation.supplement.expireDate.required}")
    @FutureOrPresent(message = "{supplement.expire.date.invalid}")
    private LocalDate expireDate;

    @NotNull(message = "{validation.supplement.type.required}")
    private RoutineCategory type;

}
