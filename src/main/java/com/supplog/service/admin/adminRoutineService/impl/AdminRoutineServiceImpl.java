package com.supplog.service.admin.adminRoutineService.impl;

import com.supplog.dto.admin.routine.UpdateRoutineRequestDtoAdmin;
import com.supplog.dto.routine.RoutineResponseDto;
import com.supplog.entity.Routine;
import com.supplog.exception.BusinessException;
import com.supplog.exception.ResourceNotFoundException;
import com.supplog.repository.RoutineRepository;
import com.supplog.repository.SupplementRepository;
import com.supplog.repository.UserRepository;
import com.supplog.service.admin.adminRoutineService.AdminRoutineService;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class AdminRoutineServiceImpl implements AdminRoutineService {

    private final UserRepository userRepository;
    private final SupplementRepository supplementRepository;
    private final RoutineRepository routineRepository;
    private final ModelMapper mapper;


    public AdminRoutineServiceImpl(
            UserRepository userRepository, SupplementRepository supplementRepository, RoutineRepository routineRepository,
            ModelMapper mapper
    ) {
        this.userRepository = userRepository;
        this.supplementRepository = supplementRepository;
        this.routineRepository = routineRepository;
        this.mapper = mapper;
    }

    @Override
    public List<RoutineResponseDto> getAll() {
        List<Routine> routines = routineRepository.findAll();
        List<RoutineResponseDto> responseDtos = new ArrayList<>();

        for (Routine routine : routines) {
            responseDtos.add(
                    mapper.map(routine, RoutineResponseDto.class)
            );
        }

        return responseDtos;
    }

    @Override
    public RoutineResponseDto getById(Long id) {
        Routine routine = findRoutineById(id);

        return mapper.map(routine, RoutineResponseDto.class);
    }

    @Override
    public List<RoutineResponseDto> getAllActiveRoutines() {
        List<Routine> activeRoutines =
                routineRepository.findAllByIsDeletedFalse();

        List<RoutineResponseDto> responseDtos = new ArrayList<>();

        for (Routine routine : activeRoutines) {
            responseDtos.add(
                    mapper.map(routine, RoutineResponseDto.class)
            );
        }

        return responseDtos;
    }

    @Override
    public List<RoutineResponseDto> getAllInactiveRoutines() {
        List<Routine> inactiveRoutines =
                routineRepository.findAllByIsDeletedTrue();

        List<RoutineResponseDto> responseDtos = new ArrayList<>();

        for (Routine routine : inactiveRoutines) {
            responseDtos.add(
                    mapper.map(routine, RoutineResponseDto.class)
            );
        }

        return responseDtos;
    }

    @Override
    public List<RoutineResponseDto> getAllRoutinesByUserId(Long userId) {
        List<Routine> routines =
                routineRepository.findAllByUserId(userId);

        List<RoutineResponseDto> responseDtos = new ArrayList<>();

        for (Routine routine : routines) {
            responseDtos.add(
                    mapper.map(routine, RoutineResponseDto.class)
            );
        }

        return responseDtos;
    }

    @Override
    public List<RoutineResponseDto> getAllRoutinesBySupplementId(Long supplementId) {
        List<Routine> routines =
                routineRepository.findAllBySupplementId(supplementId);

        List<RoutineResponseDto> responseDtos = new ArrayList<>();

        for (Routine routine : routines) {
            responseDtos.add(
                    mapper.map(routine, RoutineResponseDto.class)
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
            UpdateRoutineRequestDtoAdmin requestDto
    ) {
        Routine routine = findRoutineById(id);

        routine.setDayName(requestDto.getDayName());
        routine.setPeriod(requestDto.getPeriod());
        routine.setRoutineTime(requestDto.getRoutineTime());

    }

    // Helper method
    private Routine findRoutineById(Long id) {
        return routineRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("routine.not.found", id));
    }
}