package com.supplog.service.admin.adminSupplementService.impl;

import com.supplog.dto.supplement.SupplementResponseDto;
import com.supplog.dto.supplement.UpdateSupplementRequestDto;
import com.supplog.entity.Supplement;
import com.supplog.entity.User;
import com.supplog.exception.BusinessException;
import com.supplog.exception.ResourceNotFoundException;
import com.supplog.repository.RoutineRepository;
import com.supplog.repository.SupplementRepository;
import com.supplog.repository.UserRepository;
import com.supplog.service.admin.adminSupplementService.AdminSupplementService;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class AdminSupplementServiceImpl implements AdminSupplementService {
    private final SupplementRepository supplementRepository;
    private final RoutineRepository routineRepository;
    private final UserRepository userRepository;
    private final ModelMapper mapper;

    public AdminSupplementServiceImpl(SupplementRepository supplementRepository, RoutineRepository routineRepository, UserRepository userRepository, ModelMapper mapper) {
        this.supplementRepository = supplementRepository;
        this.routineRepository = routineRepository;
        this.userRepository = userRepository;
        this.mapper = mapper;
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
    public SupplementResponseDto getById(Long id) {
        Supplement supplement = findSupplementById(id);

        return mapper.map(supplement, SupplementResponseDto.class);
    }

    @Override
    public List<SupplementResponseDto> getAllActiveSupplements() {
        List<Supplement> allActiveSupplements = supplementRepository.findAllByIsDeletedFalse();
        List<SupplementResponseDto> allActiveSupplementDtos = new ArrayList<>();
        for (Supplement supplement : allActiveSupplements) {
            allActiveSupplementDtos.add(mapper.map(supplement, SupplementResponseDto.class));
        }
        return allActiveSupplementDtos;
    }

    @Override
    public List<SupplementResponseDto> getAllInactiveSupplements() {
        List<Supplement> allInactiveSupplements = supplementRepository.findAllByIsDeletedTrue();
        List<SupplementResponseDto> allInactiveSupplementDtos = new ArrayList<>();
        for (Supplement supplement : allInactiveSupplements) {
            allInactiveSupplementDtos.add(mapper.map(supplement, SupplementResponseDto.class));
        }
        return allInactiveSupplementDtos;
    }

    @Override
    public List<SupplementResponseDto> getAllSupplementsByUserId(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("user.not.found", userId);
        }

        List<Supplement> supplementsById = supplementRepository.findAllByInsertedByUserId(userId);

        List<SupplementResponseDto> supplementResponseDtos = new ArrayList<>();
        for (Supplement supplement : supplementsById) {
            supplementResponseDtos.add(mapper.map(supplement, SupplementResponseDto.class));
        }
        return supplementResponseDtos;
    }

    @Override
    public void activateSupplementById(Long id) {
        Supplement supplement = findSupplementById(id);
        boolean ownerIsActive = userRepository.existsByIdAndIsDeletedFalse(supplement.getInsertedByUser().getId());

        if (!ownerIsActive) {
            throw new BusinessException(
                    "supplement.cannot.restore.inactive.user"
            );
        }
        supplement.setDeleted(false);
        supplementRepository.save(supplement);

    }

    @Override
    public void deactivateSupplementById(Long id) {
        if (routineRepository.existsBySupplementIdAndDeletedFalse(id)) {
            throw new BusinessException(
                    "supplement.cannot.delete.in.use"
            );
        }
        Supplement supplement = findSupplementById(id);

        supplement.setDeleted(true);
        supplementRepository.save(supplement);

    }

    @Override
    public void updateSupplementById(Long id, UpdateSupplementRequestDto requestDto) {
        Supplement supplement = findSupplementById(id);

        supplement.setName(requestDto.getName());
        supplement.setSuppDosage(requestDto.getSuppDosage());
        supplement.setExpireDate(requestDto.getExpireDate());
        supplement.setType(requestDto.getType());

        supplementRepository.save(supplement);

    }

    //Helper
    private Supplement findSupplementById(Long id) {
        return supplementRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("supplement.not.found", id));
    }
}
