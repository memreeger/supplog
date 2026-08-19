package com.supplog.service.admin.adminSupplementService;

import com.supplog.dto.admin.supplement.AdminSupplementResponseDto;
import com.supplog.dto.admin.supplement.UpdateSupplementRequestDtoAdmin;
import com.supplog.dto.supplement.UpdateSupplementRequestDto;

import java.util.List;

public interface AdminSupplementService {

    List<AdminSupplementResponseDto> getAll();

    AdminSupplementResponseDto getById(Long id);

    List<AdminSupplementResponseDto> getAllActiveSupplements();

    List<AdminSupplementResponseDto> getAllInactiveSupplements();

    List<AdminSupplementResponseDto> getAllSupplementsByUserId(Long userId);

    void activateSupplementById(Long id);

    void deactivateSupplementById(Long id);

    void updateSupplementById(Long id, UpdateSupplementRequestDtoAdmin requestDto);
}