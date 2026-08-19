package com.supplog.service.admin.adminRoutineService.impl;

import com.supplog.dto.admin.routine.AdminRoutineResponseDto;
import com.supplog.dto.routine.UpdateRoutineRequestDto;
import com.supplog.entity.Routine;
import com.supplog.enums.DayOfWeek;
import com.supplog.exception.BusinessException;
import com.supplog.exception.ResourceNotFoundException;
import com.supplog.repository.RoutineRepository;
import com.supplog.repository.SupplementRepository;
import com.supplog.repository.UserRepository;
import com.supplog.service.admin.adminRoutineService.AdminRoutineService;
import com.supplog.service.routine.RoutineValidator;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class AdminRoutineServiceImpl implements AdminRoutineService {

    private final UserRepository userRepository;
    private final SupplementRepository supplementRepository;
    private final RoutineRepository routineRepository;
    private final ModelMapper mapper;
    private final RoutineValidator routineValidator;


    public AdminRoutineServiceImpl(
            UserRepository userRepository, SupplementRepository supplementRepository, RoutineRepository routineRepository,
            ModelMapper mapper, RoutineValidator routineValidator
    ) {
        this.userRepository = userRepository;
        this.supplementRepository = supplementRepository;
        this.routineRepository = routineRepository;
        this.mapper = mapper;
        this.routineValidator = routineValidator;
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
    public void activateRoutineById(Long id) {
        Routine routine = findRoutineById(id);

        boolean userIsActive = userRepository.existsByIdAndIsDeletedFalse(routine.getUser().getId());

        if (!userIsActive) {
            throw new BusinessException("routine.cannot.restore.inactive.user");
        }

        boolean supplementIsActive = supplementRepository.existsByIdAndIsDeletedFalse(routine.getSupplement().getId());

        if (!supplementIsActive) {
            throw new BusinessException("routine.cannot.restore.inactive.supplement");
        }
        routine.setDeleted(false);
    }

    @Override
    @Transactional
    public void deactivateRoutineById(Long id) {
        Routine routine = findRoutineById(id);

        routine.setDeleted(true);
    }

    @Override
    @Transactional
    public void updateRoutineById(
            Long id,
            UpdateRoutineRequestDto requestDto
    ) {
        Routine routine = findRoutineById(id);

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
    }


    // Helper methods
    private Routine findRoutineById(Long id) {
        return routineRepository.findById(id)
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
}