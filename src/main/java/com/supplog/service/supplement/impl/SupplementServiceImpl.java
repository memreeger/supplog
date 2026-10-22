package com.supplog.service.supplement.impl;

import com.supplog.dto.supplement.CreateSupplementRequestDto;
import com.supplog.dto.supplement.SupplementResponseDto;
import com.supplog.dto.supplement.UpdateSupplementDosageRequestDto;
import com.supplog.dto.supplement.UpdateSupplementRequestDto;
import com.supplog.entity.Routine;
import com.supplog.entity.Supplement;
import com.supplog.entity.User;
import com.supplog.exception.BusinessException;
import com.supplog.exception.ResourceNotFoundException;
import com.supplog.repository.RoutineRepository;
import com.supplog.repository.SupplementRepository;
import com.supplog.service.supplement.SupplementService;
import com.supplog.service.user.ActiveUserService;
import com.supplog.util.InputNormalizer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class SupplementServiceImpl implements SupplementService {
    private final SupplementRepository supplementRepository;
    private final ActiveUserService activeUserService;
    private final RoutineRepository routineRepository;

    public SupplementServiceImpl(SupplementRepository supplementRepository, ActiveUserService activeUserService, RoutineRepository routineRepository) {
        this.supplementRepository = supplementRepository;
        this.activeUserService = activeUserService;
        this.routineRepository = routineRepository;
    }

    @Override
    @Transactional
    public void addSupplement(Long userId, CreateSupplementRequestDto requestDto) {


        User user = activeUserService.getRequiredActiveUser(userId);
        String name = InputNormalizer.trim(requestDto.getName());

        if (supplementRepository.existsByInsertedByUserIdAndIsDeletedFalseAndNameIgnoreCase(userId, name)) {
            throw new BusinessException("supplement.name.already.exists", name);
        }

        Supplement supplement = new Supplement();
        supplement.setName(name);
        supplement.setSuppDosage(InputNormalizer.trim(requestDto.getSuppDosage()));
        supplement.setExpireDate(requestDto.getExpireDate());
        supplement.setType(requestDto.getType());
        supplement.setInsertedByUser(user);


        supplementRepository.save(supplement);
    }

    @Override
    public List<SupplementResponseDto> getMySupplements(Long userId) {
        activeUserService.requireActiveUser(userId);

        return supplementRepository.findAllByInsertedByUserIdAndIsDeletedFalse(userId)
                .stream()
                .map(this::toResponseDto)
                .toList();
    }

    @Override
    public SupplementResponseDto getMySupplementById(Long userId, Long supplementId) {
        Supplement supplement = findActiveSupplement(userId, supplementId);

        return toResponseDto(supplement);
    }

    @Override
    @Transactional
    public void updateMySupplement(Long userId, Long supplementId, UpdateSupplementRequestDto requestDto) {
        Supplement supplement = findActiveSupplement(userId, supplementId);
        String name = InputNormalizer.trim(requestDto.getName());

        if (supplementRepository.existsByInsertedByUserIdAndIsDeletedFalseAndNameIgnoreCaseAndIdNot(userId, name, supplementId)) {
            throw new BusinessException("supplement.name.already.exists", name);
        }

        supplement.setName(name);
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

        List<Routine> activeRoutines =
                routineRepository.findActiveRoutinesBySupplementId(supplementId);

        if (!activeRoutines.isEmpty()) {
            // Kullanıcıya ID değil, engelleyen rutinlerin saati ve sıklığı gösterilir.
            List<Map<String, Object>> blocking = activeRoutines.stream()
                    .map(routine -> {
                        Map<String, Object> item = new LinkedHashMap<>();
                        item.put("id", routine.getId());
                        item.put("routineTime", routine.getRoutineTime());
                        item.put("frequency", routine.getFrequency());
                        return item;
                    })
                    .toList();

            throw new BusinessException(
                    "supplement.cannot.delete.in.use",
                    activeRoutines.size()
            ).withDetails(Map.of("routines", blocking));
        }

        supplement.setDeleted(true);

    }

    //Helper methods

    private Supplement findActiveSupplement(Long userId, Long supplementId) {
        activeUserService.requireActiveUser(userId);

        return supplementRepository.findByIdAndInsertedByUserIdAndIsDeletedFalse(supplementId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("supplement.not.found", supplementId));
    }

    private SupplementResponseDto toResponseDto(Supplement supplement) {
        return new SupplementResponseDto(
                supplement.getId(),
                supplement.getName(),
                supplement.getSuppDosage(),
                supplement.getExpireDate(),
                supplement.getType(),
                supplement.getInsertedByUser().getId(),
                supplement.getCreatedAt(),
                supplement.getUpdatedAt()
        );
    }
}
