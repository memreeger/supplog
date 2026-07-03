package com.supplog.service.admin.adminRoutineService;


import com.supplog.dto.admin.routine.UpdateRoutineRequestDtoAdmin;
import com.supplog.dto.routine.RoutineResponseDto;

import java.util.List;

public interface AdminRoutineService {
    List<RoutineResponseDto> getAll();

    RoutineResponseDto getById(Long id);

    List<RoutineResponseDto> getAllActiveRoutines();

    List<RoutineResponseDto> getAllInactiveRoutines();

    List<RoutineResponseDto> getAllRoutinesByUserId(Long id);

    List<RoutineResponseDto> getAllRoutinesBySupplementId(Long id);

    void activateRoutineById(Long id);

    void deactivateRoutineById(Long id);

    void updateRoutineById(Long id, UpdateRoutineRequestDtoAdmin updateRoutineRequestDtoAdmin);


}
