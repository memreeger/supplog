package com.supplog.service.admin.adminRoutineService.impl;

import com.supplog.dto.admin.routine.UpdateRoutineRequestDtoAdmin;
import com.supplog.dto.routine.RoutineResponseDto;
import com.supplog.entity.Routine;
import com.supplog.exception.ResourceNotFoundException;
import com.supplog.repository.RoutineRepository;
import com.supplog.service.admin.adminRoutineService.AdminRoutineService;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class AdminRoutineServiceImpl implements AdminRoutineService {

    private final RoutineRepository routineRepository;
    private final ModelMapper mapper;

    public AdminRoutineServiceImpl(
            RoutineRepository routineRepository,
            ModelMapper mapper
    ) {
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
    public List<RoutineResponseDto> getAllRoutinesBySupplementId(
            Long supplementId
    ) {
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
    public void activateRoutineById(Long id) {
        Routine routine = findRoutineById(id);

        routine.setDeleted(false);
        routineRepository.save(routine);
    }

    @Override
    public void deactivateRoutineById(Long id) {
        Routine routine = findRoutineById(id);

        routine.setDeleted(true);
        routineRepository.save(routine);
    }

    @Override
    public void updateRoutineById(
            Long id,
            UpdateRoutineRequestDtoAdmin requestDto
    ) {
        Routine routine = findRoutineById(id);

        routine.setDayName(requestDto.getDayName());
        routine.setPeriod(requestDto.getPeriod());
        routine.setRoutineTime(requestDto.getRoutineTime());

        routineRepository.save(routine);
    }

    // Helper method
    private Routine findRoutineById(Long id) {
        return routineRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("routine.not.found", id));
    }
}