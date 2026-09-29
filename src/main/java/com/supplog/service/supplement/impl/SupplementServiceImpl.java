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
import com.supplog.service.supplement.SupplementService;
import com.supplog.service.user.ActiveUserService;
import com.supplog.util.InputNormalizer;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class SupplementServiceImpl implements SupplementService {
    private final SupplementRepository supplementRepository;
    private final ActiveUserService activeUserService;
    private final RoutineRepository routineRepository;
    private final ModelMapper mapper;

    public SupplementServiceImpl(SupplementRepository supplementRepository, ActiveUserService activeUserService, RoutineRepository routineRepository, ModelMapper mapper) {
        this.supplementRepository = supplementRepository;
        this.activeUserService = activeUserService;
        this.routineRepository = routineRepository;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public void addSupplement(Long userId, CreateSupplementRequestDto requestDto) {


        User user = activeUserService.getRequiredActiveUser(userId);

        Supplement supplement = new Supplement();



        supplement.setName(InputNormalizer.trim(requestDto.getName()));
        supplement.setSuppDosage(InputNormalizer.trim(requestDto.getSuppDosage()));
        supplement.setExpireDate(requestDto.getExpireDate());
        supplement.setType(requestDto.getType());
        supplement.setInsertedByUser(user);


        supplementRepository.save(supplement);
    }

    @Override
    public List<SupplementResponseDto> getMySupplements(Long userId) {
        activeUserService.requireActiveUser(userId);

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

        SupplementResponseDto dto = mapper.map(supplement, SupplementResponseDto.class);
        dto.setUserId(supplement.getInsertedByUser().getId());
        return dto;
    }

    @Override
    @Transactional
    public void updateMySupplement(Long userId, Long supplementId, UpdateSupplementRequestDto requestDto) {
        Supplement supplement = findActiveSupplement(userId, supplementId);

        supplement.setName(InputNormalizer.trim(requestDto.getName()));
        supplement.setSuppDosage(InputNormalizer.trim(requestDto.getSuppDosage()));
        supplement.setType(requestDto.getType());
        supplement.setExpireDate(requestDto.getExpireDate());


    }

    @Override
    @Transactional
    public void updateMySupplementDosage(Long userId, Long supplementId, UpdateSupplementDosageRequestDto requestDto) {
        Supplement supplement = findActiveSupplement(userId, supplementId);
        supplement.setSuppDosage(InputNormalizer.trim(requestDto.getDosage()));


    }


    @Override
    @Transactional
    public void deleteMySupplement(Long userId, Long supplementId) {
        Supplement supplement = findActiveSupplement(userId, supplementId);

        List<Long> activeRoutineIds =
                routineRepository.findActiveRoutineIdsBySupplementId(supplementId);

        if (!activeRoutineIds.isEmpty()) {
            throw new BusinessException(
                    "supplement.cannot.delete.in.use",
                    activeRoutineIds
            );
        }

        supplement.setDeleted(true);

    }

    //Helper methods

    private Supplement findActiveSupplement(Long userId, Long supplementId) {
        activeUserService.requireActiveUser(userId);

        return supplementRepository.findByIdAndInsertedByUserIdAndIsDeletedFalse(supplementId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("supplement.not.found", supplementId));
    }
}
