package com.supplog.service.admin.adminSupplementService;

import com.supplog.dto.admin.supplement.AdminSupplementResponseDto;
import com.supplog.dto.admin.supplement.AdminSupplementDetailResponseDto;
import com.supplog.dto.admin.supplement.UpdateSupplementRequestDtoAdmin;
import com.supplog.enums.RoutineCategory;

import java.util.List;

public interface AdminSupplementService {

    List<AdminSupplementResponseDto> getAll();

    AdminSupplementResponseDto getById(Long id);

    AdminSupplementDetailResponseDto getDetail(Long id);

    List<AdminSupplementResponseDto> search(
            Long userId,
            String name,
            RoutineCategory type,
            Boolean active
    );

    List<AdminSupplementResponseDto> getAllActiveSupplements();

    List<AdminSupplementResponseDto> getAllInactiveSupplements();

    List<AdminSupplementResponseDto> getAllSupplementsByUserId(Long userId);

    void activateSupplementById(Long adminId, Long id, String reason);

    void deactivateSupplementById(Long adminId, Long id, String reason);

    void updateSupplementById(Long adminId, Long id, UpdateSupplementRequestDtoAdmin requestDto);
}
