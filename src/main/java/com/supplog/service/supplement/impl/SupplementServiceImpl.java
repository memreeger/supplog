package com.supplog.service.supplement.impl;

import com.supplog.dto.supplement.CreateSupplementRequestDto;
import com.supplog.dto.supplement.SupplementResponseDto;
import com.supplog.dto.supplement.UpdateSupplementDosageRequestDto;
import com.supplog.dto.supplement.UpdateSupplementRequestDto;
import com.supplog.entity.Supplement;
import com.supplog.entity.User;
import com.supplog.exception.BusinessException;
import com.supplog.exception.ResourceNotFoundException;
import com.supplog.repository.RoutineRepository;
import com.supplog.repository.SupplementRepository;
import com.supplog.repository.UserRepository;
import com.supplog.service.supplement.SupplementService;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class SupplementServiceImpl implements SupplementService {
    private final SupplementRepository supplementRepository;
    private final UserRepository userRepository;
    private final RoutineRepository routineRepository;
    private final ModelMapper mapper;

    public SupplementServiceImpl(SupplementRepository supplementRepository, UserRepository userRepository, RoutineRepository routineRepository, ModelMapper mapper) {
        this.supplementRepository = supplementRepository;
        this.userRepository = userRepository;
        this.routineRepository = routineRepository;
        this.mapper = mapper;
    }

    @Override
    public void addSupplement(Long userId, CreateSupplementRequestDto requestDto) {


        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("supplement.user.not.found", userId));

        Supplement supplement = new Supplement();


        supplement.setName(requestDto.getName());
        supplement.setSuppDosage(requestDto.getSuppDosage());
        supplement.setExpireDate(requestDto.getExpireDate());
        supplement.setType(requestDto.getType());
        supplement.setInsertedByUser(user);

        supplementRepository.save(supplement);
    }

    @Override
    public List<SupplementResponseDto> getMySupplements(Long userId) {
        List<Supplement> supplements = supplementRepository.findAllByInsertedByUserIdAndIsDeletedFalse(userId);
        List<SupplementResponseDto> supplementResponseDtos = new ArrayList<>();

        for (Supplement supplement : supplements) {
            supplementResponseDtos.add(mapper.map(supplement, SupplementResponseDto.class));
        }
        return supplementResponseDtos;
    }

    @Override
    public SupplementResponseDto getMySupplementById(Long userId, Long supplementId) {
        Supplement supplement = findActiveSupplement(userId, supplementId);
        return mapper.map(supplement, SupplementResponseDto.class);
    }

    @Override
    public void updateMySupplement(Long userId, Long supplementId, UpdateSupplementRequestDto requestDto) {
        Supplement supplement = findActiveSupplement(userId, supplementId);
        supplement.setName(requestDto.getName());
        supplement.setSuppDosage(requestDto.getSuppDosage());
        supplement.setType(requestDto.getType());
        supplement.setExpireDate(requestDto.getExpireDate());

        supplementRepository.save(supplement);

    }

    @Override
    public void updateMySupplementDosage(Long userId, Long supplementId, UpdateSupplementDosageRequestDto requestDto) {
        Supplement supplement = findActiveSupplement(userId, supplementId);
        supplement.setSuppDosage(requestDto.getDosage());

        supplementRepository.save(supplement);

    }


    @Override
    public void deleteMySupplement(Long userId, Long supplementId) {
        Supplement supplement = findActiveSupplement(userId, supplementId);

        boolean hasActiveRoutine = routineRepository.existsBySupplementIdAndUserIdAndDeletedFalse(supplementId,userId);
        if(hasActiveRoutine){
            throw new BusinessException("supplement.cannot.delete.in.use");
        }

        supplement.setDeleted(true);
        supplementRepository.save(supplement);

    }
/*
    @Override
    public SupplementResponseDto getById(Long id) {
        Supplement supplement = findSupplementById(id);

        SupplementResponseDto dto = mapper.map(supplement, SupplementResponseDto.class);
        return dto;
    }


    @Override
    public List<SupplementResponseDto> getAll() {
        List<Supplement> allSupplements = supplementRepository.findAll();
        List<SupplementResponseDto> allSupplementDtos = new ArrayList<>();
        for (Supplement supplement : allSupplements) {
            allSupplementDtos.add(mapper.map(supplement, SupplementResponseDto.class));
        }
        return allSupplementDtos;
    }

    @Override
    public List<SupplementResponseDto> getAllByUserIdIsDeletedFalse(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("user.not.found", userId);
        }
        List<Supplement> supplements =
                supplementRepository.findAllByInsertedByUserIdAndIsDeletedFalse(userId);

        List<SupplementResponseDto> dtos = new ArrayList<>();
        for (Supplement supplement : supplements) {
            dtos.add(mapper.map(supplement, SupplementResponseDto.class));
        }

        return dtos;
    }

    @Override
    public List<SupplementResponseDto> findAllByInsertedByUserId(Long userId) {
        if (userId < 0) {
            throw new BusinessException("user.id.invalid", userId);
        }

        List<Supplement> supplements =
                supplementRepository.findAllByInsertedByUserId(userId);
        List<SupplementResponseDto> dtos = new ArrayList<>();

        for (Supplement supplement : supplements) {
            dtos.add(mapper.map(supplement, SupplementResponseDto.class));
        }
        return dtos;

    }

    @Override
    public void updateSupplement(Long id, UpdateSupplementRequestDto requestDto) {
        Supplement supplement = findSupplementById(id);
        supplement.setName(requestDto.getName());
        supplement.setSuppDosage(requestDto.getSuppDosage());
        supplement.setExpireDate(requestDto.getExpireDate());
        supplement.setType(requestDto.getType());

        supplementRepository.save(supplement);
    }

    @Override
    public void updateSupplementDosage(Long id, UpdateSupplementDosageRequestDto requestDto) {
        Supplement supplement = findSupplementById(id);
        supplement.setSuppDosage(requestDto.getDosage());

        supplementRepository.save(supplement);
    }

    @Override
    public void deleteSupplement(Long id) {
        Supplement supplement = findSupplementById(id);
        supplement.setDeleted(true);
        supplementRepository.save(supplement);
    }

 */

    //Helper methods
    public Supplement findByName(String name) {
        return supplementRepository.findSupplementByName(name)
                .orElseThrow(() -> new ResourceNotFoundException("supplement.not.found", name));
    }

    private Supplement findSupplementById(Long id) {
        return supplementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("supplement.not.found", id));
    }

    private Supplement findActiveSupplement(Long userId, Long supplementId) {
        Supplement supplement = supplementRepository.findByIdAndInsertedByUserIdAndIsDeletedFalse(supplementId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("supplement.not.found", supplementId));
        return supplement;
    }
}
