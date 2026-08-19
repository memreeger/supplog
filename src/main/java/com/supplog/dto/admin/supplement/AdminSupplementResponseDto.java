package com.supplog.dto.admin.supplement;

import com.supplog.dto.supplement.SupplementResponseDto;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class AdminSupplementResponseDto extends SupplementResponseDto {
    private boolean deleted;
}
