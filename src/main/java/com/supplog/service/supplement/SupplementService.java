package com.supplog.service.supplement;

import com.supplog.dto.supplement.CreateSupplementRequestDto;
import com.supplog.dto.supplement.SupplementResponseDto;
import com.supplog.dto.supplement.UpdateSupplementDosageRequestDto;
import com.supplog.dto.supplement.UpdateSupplementRequestDto;

import java.util.List;

public interface SupplementService {
    void addSupplement(Long userId, CreateSupplementRequestDto requestDto);

    List<SupplementResponseDto> getMySupplements(Long userId);

    SupplementResponseDto getMySupplementById(Long userId, Long supplementId);

    void updateMySupplement(Long userId, Long supplementId, UpdateSupplementRequestDto requestDto);

    void updateMySupplementDosage(Long userId, Long supplementId, UpdateSupplementDosageRequestDto requestDto);

    void deleteMySupplement(Long userId, Long supplementId);

    //CRUD SoftDelete
}
