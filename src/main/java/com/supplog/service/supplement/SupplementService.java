package com.supplog.service.supplement;

import com.supplog.dto.supplement.CreateSupplementRequestDto;
import com.supplog.dto.supplement.SupplementResponseDto;
import com.supplog.dto.supplement.UpdateSupplementDosageRequestDto;
import com.supplog.dto.supplement.UpdateSupplementRequestDto;
import com.supplog.entity.Supplement;
import org.springframework.stereotype.Service;

import java.util.List;

public interface SupplementService {
    void addSupplement(CreateSupplementRequestDto supplementRequestDto);

    SupplementResponseDto getById(Long id);

    List<SupplementResponseDto> getAll();

    List<SupplementResponseDto> getAllByUserIdIsDeletedFalse(Long userId);

    List<SupplementResponseDto> findAllByInsertedByUserId(Long userId);

    void updateSupplement(Long id, UpdateSupplementRequestDto requestDto);

    void updateSupplementDosage(Long id, UpdateSupplementDosageRequestDto requestDto);

    void deleteSupplement(Long id);


    //CRUD SoftDelete
}
