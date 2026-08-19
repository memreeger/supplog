package com.supplog.service.admin.adminRoutineService;


import com.supplog.dto.admin.routine.AdminRoutineResponseDto;
import com.supplog.dto.routine.UpdateRoutineRequestDto;

import java.util.List;

public interface AdminRoutineService {
    List<AdminRoutineResponseDto> getAll();

    AdminRoutineResponseDto getById(Long id);

    List<AdminRoutineResponseDto> getAllActiveRoutines();

    List<AdminRoutineResponseDto> getAllInactiveRoutines();

    List<AdminRoutineResponseDto> getAllRoutinesByUserId(Long id);

    List<AdminRoutineResponseDto> getAllRoutinesBySupplementId(Long id);

    void activateRoutineById(Long id);

    void deactivateRoutineById(Long id);

    void updateRoutineById(
            Long id,
            UpdateRoutineRequestDto requestDto
    );


}
