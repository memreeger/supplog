package com.supplog.dto.supplement;

import com.supplog.enums.SupplementCategory;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class UpdateSupplementRequestDto extends BaseSupplementDto {
    private String suppDosage;
    private LocalDate expireDate;
    private SupplementCategory type;
}
