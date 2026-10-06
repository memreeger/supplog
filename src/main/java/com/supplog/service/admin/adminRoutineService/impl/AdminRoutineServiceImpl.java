package com.supplog.service.admin.adminRoutineService.impl;

import com.supplog.dto.admin.routine.AdminRoutineResponseDto;
import com.supplog.dto.admin.routine.AdminRoutineDetailResponseDto;
import com.supplog.dto.admin.routine.AdminRoutineUpdateRequestDto;
import com.supplog.entity.Routine;
import com.supplog.enums.DayOfWeek;
import com.supplog.enums.Frequency;
import com.supplog.exception.BusinessException;
import com.supplog.exception.ResourceNotFoundException;
import com.supplog.repository.RoutineRepository;
import com.supplog.repository.SupplementRepository;
import com.supplog.repository.UserRepository;
import com.supplog.service.admin.adminRoutineService.AdminRoutineService;
import com.supplog.service.admin.adminExecutionService.AdminExecutionService;
import com.supplog.service.admin.audit.AdminAuditService;
import com.supplog.service.routine.RoutineValidator;
import com.supplog.service.routineExecution.RoutineExecutionService;
import com.supplog.service.support.SupportService;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.Collections;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class AdminRoutineServiceImpl implements AdminRoutineService {

    private final UserRepository userRepository;
    private final SupplementRepository supplementRepository;
    private final RoutineRepository routineRepository;
    private final ModelMapper mapper;
    private final RoutineValidator routineValidator;
    private final SupportService supportService;
    private final AdminExecutionService adminExecutionService;
    private final AdminAuditService auditService;
    private final RoutineExecutionService routineExecutionService;


    public AdminRoutineServiceImpl(
            UserRepository userRepository, SupplementRepository supplementRepository, RoutineRepository routineRepository,
            ModelMapper mapper, RoutineValidator routineValidator, SupportService supportService,
            AdminExecutionService adminExecutionService, AdminAuditService auditService,
            RoutineExecutionService routineExecutionService
    ) {
        this.userRepository = userRepository;
        this.supplementRepository = supplementRepository;
        this.routineRepository = routineRepository;
        this.mapper = mapper;
        this.routineValidator = routineValidator;
        this.supportService = supportService;
        this.adminExecutionService = adminExecutionService;
        this.auditService = auditService;
        this.routineExecutionService = routineExecutionService;
    }

    @Override
    public List<AdminRoutineResponseDto> getAll() {
        List<Routine> routines = routineRepository.findAll();
        List<AdminRoutineResponseDto> responseDtos = new ArrayList<>();

        for (Routine routine : routines) {
            responseDtos.add(
                    toAdminRoutineResponseDto(routine)
            );
        }

        return responseDtos;
    }

    @Override
    public AdminRoutineResponseDto getById(Long id) {
        Routine routine = findRoutineById(id);

        return toAdminRoutineResponseDto(routine);
    }

    @Override
    public AdminRoutineDetailResponseDto getDetail(Long id) {
        return new AdminRoutineDetailResponseDto(
                getById(id),
                adminExecutionService.search(
                        null, id, null, null, null, PageRequest.of(0, 20)
                ).getContent()
        );
    }

    @Override
    public Page<AdminRoutineResponseDto> search(
            Long userId,
            Long supplementId,
            Frequency frequency,
            Boolean active,
            Pageable pageable
    ) {
        Page<Long> idPage = routineRepository
                .searchAdminRoutineIds(userId, supplementId, frequency, active, pageable);

        Map<Long, Routine> routinesById = idPage.isEmpty()
                ? Collections.emptyMap()
                : routineRepository.findAllByIdInWithAdminDetails(idPage.getContent())
                        .stream()
                        .collect(Collectors.toMap(Routine::getId, Function.identity()));

        List<AdminRoutineResponseDto> content = idPage.getContent().stream()
                .map(routinesById::get)
                .map(this::toAdminRoutineResponseDto)
                .toList();

        return new PageImpl<>(content, pageable, idPage.getTotalElements());
    }

    @Override
    public List<AdminRoutineResponseDto> getAllActiveRoutines() {
        List<Routine> activeRoutines =
                routineRepository.findAllByIsDeletedFalse();

        List<AdminRoutineResponseDto> responseDtos = new ArrayList<>();

        for (Routine routine : activeRoutines) {
            responseDtos.add(
                    toAdminRoutineResponseDto(routine)
            );
        }

        return responseDtos;
    }

    @Override
    public List<AdminRoutineResponseDto> getAllInactiveRoutines() {
        List<Routine> inactiveRoutines =
                routineRepository.findAllByIsDeletedTrue();

        List<AdminRoutineResponseDto> responseDtos = new ArrayList<>();

        for (Routine routine : inactiveRoutines) {
            responseDtos.add(
                    toAdminRoutineResponseDto(routine)
            );
        }

        return responseDtos;
    }

    @Override
    public List<AdminRoutineResponseDto> getAllRoutinesByUserId(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException(
                    "user.not.found",
                    userId
            );
        }

        List<Routine> routines =
                routineRepository.findAllByUserId(userId);

        List<AdminRoutineResponseDto> responseDtos = new ArrayList<>();

        for (Routine routine : routines) {
            responseDtos.add(
                    toAdminRoutineResponseDto(routine)
            );
        }

        return responseDtos;
    }

    @Override
    public List<AdminRoutineResponseDto> getAllRoutinesBySupplementId(Long supplementId) {
        if (!supplementRepository.existsById(supplementId)) {
            throw new ResourceNotFoundException(
                    "supplement.not.found",
                    supplementId
            );
        }
        List<Routine> routines =
                routineRepository.findAllBySupplementId(supplementId);

        List<AdminRoutineResponseDto> responseDtos = new ArrayList<>();

        for (Routine routine : routines) {
            responseDtos.add(
                    toAdminRoutineResponseDto(routine)
            );
        }

        return responseDtos;
    }

    @Override
    @Transactional
    public void activateRoutineById(Long adminId, Long id, String reason) {
        Routine routine = findRoutineByIdForUpdate(id);

        if (!routine.isDeleted()) {
            throw new BusinessException("routine.already.active");
        }

        boolean userIsActive = userRepository.existsByIdAndIsDeletedFalse(routine.getUser().getId());

        if (!userIsActive) {
            throw new BusinessException("routine.cannot.restore.inactive.user");
        }

        boolean supplementIsActive = supplementRepository.existsByIdAndIsDeletedFalse(routine.getSupplement().getId());

        if (!supplementIsActive) {
            throw new BusinessException("routine.cannot.restore.inactive.supplement");
        }
        routine.setDeleted(false);

        auditService.record(
                adminId, "ROUTINE_ACTIVATED", "ROUTINE", id,
                "active=false", "active=true", reason, true, null
        );
    }

    @Override
    @Transactional
    public void deactivateRoutineById(Long adminId, Long id, String reason) {
        Routine routine = findRoutineByIdForUpdate(id);

        if (routine.isDeleted()) {
            throw new BusinessException("routine.already.deleted");
        }

        supportService.handleRoutineSoftDelete(routine.getId());
        routineExecutionService.handleRoutineSoftDelete(routine);

        routine.setDeleted(true);

        auditService.record(
                adminId, "ROUTINE_DEACTIVATED", "ROUTINE", id,
                "active=true", "active=false", reason, true, null
        );
    }

    @Override
    @Transactional
    public void updateRoutineById(
            Long adminId,
            Long id,
            AdminRoutineUpdateRequestDto requestDto
    ) {
        Routine routine = findRoutineByIdForUpdate(id);

        String oldValue = routineSummary(routine);

        LocalDate startDate =
                requestDto.getStartDate() != null
                        ? requestDto.getStartDate()
                        : routine.getStartDate();

        routineValidator.validateSchedule(
                requestDto.getFrequency(),
                requestDto.getDaysOfWeek(),
                requestDto.getDayOfMonth(),
                requestDto.getRoutineTime()
        );

        routineValidator.validateDuration(
                requestDto.getDurationType(),
                startDate,
                requestDto.getEndDate()
        );

        routine.setFrequency(requestDto.getFrequency());
        routine.setDurationType(requestDto.getDurationType());

        updateRoutineDaysCollection(
                routine,
                requestDto.getDaysOfWeek()
        );

        routine.setDayOfMonth(requestDto.getDayOfMonth());
        routine.setRoutineTime(requestDto.getRoutineTime());
        routine.setStartDate(startDate);
        routine.setEndDate(requestDto.getEndDate());

        if (requestDto.getMissedGracePeriod() != null) {
            routine.setMissedGracePeriod(requestDto.getMissedGracePeriod());
        }

        routineExecutionService.synchronizePendingAfterRoutineUpdate(routine);

        auditService.record(
                adminId,
                "ROUTINE_UPDATED",
                "ROUTINE",
                id,
                oldValue,
                routineSummary(routine),
                requestDto.getReason(),
                true,
                null
        );
    }


    // Helper methods
    private Routine findRoutineById(Long id) {
        return routineRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("routine.not.found", id));
    }

    private Routine findRoutineByIdForUpdate(Long id) {
        return routineRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("routine.not.found", id));
    }

    private AdminRoutineResponseDto toAdminRoutineResponseDto(Routine routine) {

        AdminRoutineResponseDto dto = mapper.map(routine, AdminRoutineResponseDto.class);

        dto.setDeleted(routine.isDeleted());
        dto.setUserId(routine.getUser().getId());
        dto.setSupplementId(routine.getSupplement().getId());
        dto.setSupplementName(routine.getSupplement().getName());

        return dto;
    }

    private void updateRoutineDaysCollection(
            Routine routine,
            Set<DayOfWeek> daysOfWeek
    ) {
        routine.getDaysOfWeek().clear();

        if (daysOfWeek != null) {
            routine.getDaysOfWeek().addAll(daysOfWeek);
        }
    }

    private String routineSummary(Routine routine) {
        return "frequency=" + routine.getFrequency()
                + ",days=" + routine.getDaysOfWeek()
                + ",dayOfMonth=" + routine.getDayOfMonth()
                + ",time=" + routine.getRoutineTime()
                + ",startDate=" + routine.getStartDate()
                + ",endDate=" + routine.getEndDate()
                + ",deleted=" + routine.isDeleted();
    }
}
