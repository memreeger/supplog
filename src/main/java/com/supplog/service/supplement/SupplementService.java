package com.supplog.service.supplement;

import com.supplog.dto.supplement.CreateSupplementRequestDto;
import com.supplog.dto.supplement.SupplementResponseDto;
import com.supplog.dto.supplement.UpdateSupplementDosageRequestDto;
import com.supplog.dto.supplement.UpdateSupplementRequestDto;
import com.supplog.entity.Supplement;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface SupplementService {
    void addSupplement(CreateSupplementRequestDto supplementRequestDto);

    SupplementResponseDto getById(Long id);

    List<SupplementResponseDto> getAll();

    List<SupplementResponseDto> getAllByUserIdIsDeletedFalse(Long userId);

    List<SupplementResponseDto> findAllByInsertedByUserId(Long userId);

    void updateSupplement(String name, UpdateSupplementRequestDto requestDto);

    void updateSupplementDosage(String name, UpdateSupplementDosageRequestDto requestDto);

    void deleteSupplement(String name);


    //CRUD SoftDelete
}
