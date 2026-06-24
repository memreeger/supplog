package com.supplog.dto.supplement;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class UpdateSupplementDosageRequestDto {
    @NotBlank(message = "{validation.supplement.dosage.required}")
    private String dosage;
}
