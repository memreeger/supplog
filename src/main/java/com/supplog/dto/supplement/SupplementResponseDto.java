package com.supplog.dto.supplement;

import com.supplog.enums.SupplementCategory;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class SupplementResponseDto extends BaseSupplementDto {
    private Long id;
    private String suppDosage;
    private LocalDate expireDate;
    private SupplementCategory type;
    private Long userId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
