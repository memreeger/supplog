package com.supplog.dto.supplement;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;


@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class BaseSupplementDto {
    @NotBlank(message = "{validation.supplement.name.required}")
    @Size(min = 2, max = 45, message = "{validation.supplement.name.size}")
    private String name;
}
