package com.supplog.service.admin.adminRoutineService;


import com.supplog.dto.admin.routine.AdminRoutineResponseDto;
import com.supplog.dto.admin.routine.AdminRoutineDetailResponseDto;
import com.supplog.dto.admin.routine.AdminRoutineUpdateRequestDto;
import com.supplog.enums.Frequency;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AdminRoutineService {
    List<AdminRoutineResponseDto> getAll();

    AdminRoutineResponseDto getById(Long id);

    AdminRoutineDetailResponseDto getDetail(Long id);

    Page<AdminRoutineResponseDto> search(
            Long userId,
            Long supplementId,
            Frequency frequency,
            Boolean active,
            Pageable pageable
    );

    List<AdminRoutineResponseDto> getAllActiveRoutines();

    List<AdminRoutineResponseDto> getAllInactiveRoutines();

    List<AdminRoutineResponseDto> getAllRoutinesByUserId(Long id);

    List<AdminRoutineResponseDto> getAllRoutinesBySupplementId(Long id);

    void activateRoutineById(Long adminId, Long id, String reason);

    void deactivateRoutineById(Long adminId, Long id, String reason);

    void updateRoutineById(
            Long adminId,
            Long id,
            AdminRoutineUpdateRequestDto requestDto
    );


}
