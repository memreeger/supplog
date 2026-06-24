package com.supplog.dto.supplement;

import com.supplog.enums.RoutineCategory;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class SupplementResponseDto {
    private Long id;
    private String name;
    private String suppDosage;
    private LocalDate expireDate;
    private RoutineCategory type;
    private Long userId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
