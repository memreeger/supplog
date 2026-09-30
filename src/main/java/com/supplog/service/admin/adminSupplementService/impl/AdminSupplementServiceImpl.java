package com.supplog.service.admin.adminSupplementService.impl;

import com.supplog.dto.admin.supplement.AdminSupplementResponseDto;
import com.supplog.dto.admin.supplement.AdminSupplementDetailResponseDto;
import com.supplog.dto.admin.supplement.UpdateSupplementRequestDtoAdmin;
import com.supplog.dto.supplement.UpdateSupplementRequestDto;
import com.supplog.entity.Supplement;
import com.supplog.exception.BusinessException;
import com.supplog.exception.ResourceNotFoundException;
import com.supplog.enums.RoutineCategory;
import com.supplog.repository.RoutineRepository;
import com.supplog.repository.SupplementRepository;
import com.supplog.repository.UserRepository;
import com.supplog.service.admin.adminSupplementService.AdminSupplementService;
import com.supplog.service.admin.adminRoutineService.AdminRoutineService;
import com.supplog.service.admin.audit.AdminAuditService;
import com.supplog.util.InputNormalizer;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class AdminSupplementServiceImpl implements AdminSupplementService {
    private final SupplementRepository supplementRepository;
    private final RoutineRepository routineRepository;
    private final UserRepository userRepository;
    private final ModelMapper mapper;
    private final AdminRoutineService adminRoutineService;
    private final AdminAuditService auditService;

    public AdminSupplementServiceImpl(
            SupplementRepository supplementRepository,
            RoutineRepository routineRepository,
            UserRepository userRepository,
            ModelMapper mapper,
            AdminRoutineService adminRoutineService,
            AdminAuditService auditService
    ) {
        this.supplementRepository = supplementRepository;
        this.routineRepository = routineRepository;
        this.userRepository = userRepository;
        this.mapper = mapper;
        this.adminRoutineService = adminRoutineService;
        this.auditService = auditService;
    }


    @Override
    public List<AdminSupplementResponseDto> getAll() {
        List<Supplement> allSupplements = supplementRepository.findAll();
        List<AdminSupplementResponseDto> allSupplementDtos = new ArrayList<>();

        for (Supplement supplement : allSupplements) {
            allSupplementDtos.add(toAdminSupplementResponseDto(supplement));
        }
        return allSupplementDtos;
    }

    @Override
    public AdminSupplementResponseDto getById(Long id) {
        Supplement supplement = findSupplementById(id);

        return toAdminSupplementResponseDto(supplement);
    }

    @Override
    public AdminSupplementDetailResponseDto getDetail(Long id) {
        return new AdminSupplementDetailResponseDto(
                getById(id),
                adminRoutineService.getAllRoutinesBySupplementId(id)
        );
    }

    @Override
    public Page<AdminSupplementResponseDto> search(
            Long userId,
            String name,
            RoutineCategory type,
            Boolean active,
            Pageable pageable
    ) {
        String nameFilter = InputNormalizer.trim(name);
        if (nameFilter != null && nameFilter.isBlank()) {
            nameFilter = null;
        }

        return supplementRepository
                .searchAdminSupplements(userId, nameFilter, type, active, pageable)
                .map(this::toAdminSupplementResponseDto);
    }

    @Override
    public List<AdminSupplementResponseDto> getAllActiveSupplements() {
        List<Supplement> allActiveSupplements = supplementRepository.findAllByIsDeletedFalse();
        List<AdminSupplementResponseDto> allActiveSupplementDtos = new ArrayList<>();
        for (Supplement supplement : allActiveSupplements) {
            allActiveSupplementDtos.add(toAdminSupplementResponseDto(supplement));
        }
        return allActiveSupplementDtos;
    }

    @Override
    public List<AdminSupplementResponseDto> getAllInactiveSupplements() {
        List<Supplement> allInactiveSupplements = supplementRepository.findAllByIsDeletedTrue();
        List<AdminSupplementResponseDto> allInactiveSupplementDtos = new ArrayList<>();
        for (Supplement supplement : allInactiveSupplements) {
            allInactiveSupplementDtos.add(toAdminSupplementResponseDto(supplement));
        }
        return allInactiveSupplementDtos;
    }

    @Override
    public List<AdminSupplementResponseDto> getAllSupplementsByUserId(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("user.not.found", userId);
        }

        List<Supplement> supplementsById = supplementRepository.findAllByInsertedByUserId(userId);

        List<AdminSupplementResponseDto> supplementResponseDtos = new ArrayList<>();
        for (Supplement supplement : supplementsById) {
            supplementResponseDtos.add(toAdminSupplementResponseDto(supplement));
        }
        return supplementResponseDtos;
    }

    @Override
    @Transactional
    public void activateSupplementById(Long adminId, Long id, String reason) {
        Supplement supplement = findSupplementById(id);

        if (!supplement.isDeleted()) {
            throw new BusinessException("supplement.already.active");
        }

        boolean ownerIsActive = userRepository.existsByIdAndIsDeletedFalse(supplement.getInsertedByUser().getId());

        if (!ownerIsActive) {
            throw new BusinessException(
                    "supplement.cannot.restore.inactive.user"
            );
        }
        supplement.setDeleted(false);

        auditService.record(
                adminId, "SUPPLEMENT_ACTIVATED", "SUPPLEMENT", id,
                "active=false", "active=true", reason, true, null
        );

    }

    @Override
    @Transactional
    public void deactivateSupplementById(Long adminId, Long id, String reason) {
        Supplement supplement = findSupplementById(id);

        if (supplement.isDeleted()) {
            throw new BusinessException("supplement.already.deleted");
        }

        List<Long> activeRoutineIds =
                routineRepository.findActiveRoutineIdsBySupplementId(id);

        if (!activeRoutineIds.isEmpty()) {
            throw new BusinessException(
                    "supplement.cannot.delete.in.use",
                    activeRoutineIds
            );
        }

        supplement.setDeleted(true);

        auditService.record(
                adminId, "SUPPLEMENT_DEACTIVATED", "SUPPLEMENT", id,
                "active=true", "active=false", reason, true, null
        );

    }

    @Override
    @Transactional
    public void updateSupplementById(Long adminId, Long id, UpdateSupplementRequestDtoAdmin requestDto) {
        Supplement supplement = findSupplementById(id);

        String oldValue = supplementSummary(supplement);

        supplement.setName(InputNormalizer.trim(requestDto.getName()));
        supplement.setSuppDosage(InputNormalizer.trim(requestDto.getSuppDosage()));
        supplement.setExpireDate(requestDto.getExpireDate());
        supplement.setType(requestDto.getType());

        auditService.record(
                adminId,
                "SUPPLEMENT_UPDATED",
                "SUPPLEMENT",
                id,
                oldValue,
                supplementSummary(supplement),
                requestDto.getReason(),
                true,
                null
        );


    }

    //Helper
    private Supplement findSupplementById(Long id) {
        return supplementRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("supplement.not.found", id));
    }

    private AdminSupplementResponseDto toAdminSupplementResponseDto(
            Supplement supplement
    ) {

        AdminSupplementResponseDto dto =
                mapper.map(supplement, AdminSupplementResponseDto.class);

        dto.setDeleted(supplement.isDeleted());

        dto.setUserId(
                supplement.getInsertedByUser().getId()
        );

        return dto;
    }

    private String supplementSummary(Supplement supplement) {
        return "name=" + supplement.getName()
                + ",dosage=" + supplement.getSuppDosage()
                + ",expireDate=" + supplement.getExpireDate()
                + ",type=" + supplement.getType()
                + ",deleted=" + supplement.isDeleted();
    }
}
