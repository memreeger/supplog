package com.supplog.service.admin.adminSupplementService;

import com.supplog.dto.supplement.SupplementResponseDto;
import com.supplog.dto.supplement.UpdateSupplementRequestDto;

import java.util.List;

public interface AdminSupplementService {

    List<SupplementResponseDto> getAll();

    SupplementResponseDto getById(Long id);

    List<SupplementResponseDto> getAllActiveSupplements();

    List<SupplementResponseDto> getAllInactiveSupplements();

    List<SupplementResponseDto> getAllSupplementsByUserId(Long userId);

    void activateSupplementById(Long id);

    void deactivateSupplementById(Long id);

    void updateSupplementById(Long id, UpdateSupplementRequestDto requestDto);
}