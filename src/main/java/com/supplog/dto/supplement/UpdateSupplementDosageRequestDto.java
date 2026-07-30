package com.supplog.dto.supplement;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class UpdateSupplementDosageRequestDto {
    @NotBlank(message = "{validation.supplement.dosage.required}")
    @Size(max = 100, message = "{validation.supplement.dosage.size}")
    private String dosage;
}
