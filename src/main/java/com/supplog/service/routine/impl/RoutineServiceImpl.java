package com.supplog.service.routine.impl;

import com.supplog.dto.routine.*;
import com.supplog.entity.Routine;
import com.supplog.entity.Supplement;
import com.supplog.entity.User;
import com.supplog.exception.BusinessException;
import com.supplog.exception.ResourceNotFoundException;
import com.supplog.repository.RoutineRepository;
import com.supplog.repository.SupplementRepository;
import com.supplog.repository.UserRepository;
import com.supplog.service.routine.RoutineService;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class RoutineServiceImpl implements RoutineService {
    private final RoutineRepository routineRepository;
    private final SupplementRepository supplementRepository;
    private final UserRepository userRepository;
    private final ModelMapper mapper;

    public RoutineServiceImpl(RoutineRepository routineRepository, ModelMapper mapper, UserRepository userRepository, SupplementRepository supplementRepository) {

        this.routineRepository = routineRepository;
        this.supplementRepository = supplementRepository;
        this.userRepository = userRepository;
        this.mapper = mapper;
    }


    @Override
    public void addRoutine(Long userId, CreateRoutineRequestDto routineRequestDto) {
        User user = findActiveUser(userId);
        Supplement supplement = findMyActiveSupplement(userId, routineRequestDto.getSupplementId());
        Routine routine = new Routine();

        routine.setUser(user);
        routine.setSupplement(supplement);
        routine.setDayName(routineRequestDto.getDayName());
        routine.setRoutineTime(routineRequestDto.getRoutineTime());
        routine.setPeriod(routineRequestDto.getPeriod());
        routine.setDeleted(false);

        routineRepository.save(routine);

    }

    @Override
    public List<RoutineResponseDto> getMyRoutines(Long userId) {
        List<Routine> allRoutines = routineRepository.findAllByUserIdAndIsDeletedFalse(userId);
        List<RoutineResponseDto> allDtoList = new ArrayList<>();

        for (Routine routine : allRoutines) {
            allDtoList.add(mapper.map(routine, RoutineResponseDto.class));
        }
        return allDtoList;
    }

    @Override
    public RoutineResponseDto getMyRoutineById(Long userId, Long routineId) {
        Routine routine = findMyActiveRoutine(userId, routineId);
        return mapper.map(routine, RoutineResponseDto.class);

    }

    @Override
    public void updateRoutineTime(Long userId, Long routineId, UpdateRoutineTimeRequestDto requestDto) {
        Routine routine = findMyActiveRoutine(userId, routineId);
        routine.setRoutineTime(requestDto.getRoutineTime());
        routineRepository.save(routine);

    }

    @Override
    public void updateRoutineDay(Long userId, Long routineId, UpdateRoutineDayRequestDto requestDto) {
        Routine routine = findMyActiveRoutine(userId, routineId);
        routine.setDayName(requestDto.getDayName());
        routineRepository.save(routine);
    }

    @Override
    public void updateRoutinePeriod(Long userId, Long routineId, UpdateRoutinePeriodRequestDto requestDto) {
        Routine routine = findMyActiveRoutine(userId, routineId);
        routine.setPeriod(requestDto.getPeriod());
        routineRepository.save(routine);

    }

    @Override
    public void deleteRoutine(Long userId, Long routineId) {
        Routine routine = findMyActiveRoutine(userId, routineId);
        routine.setDeleted(true);
        routineRepository.save(routine);

    }

    //Helper method
    private User findActiveUser(Long userId) {
        return userRepository.findByIdAndIsDeletedFalse(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "user.not.found",
                                userId
                        )
                );
    }

    private Supplement findMyActiveSupplement(
            Long userId,
            Long supplementId
    ) {
        return supplementRepository
                .findByIdAndInsertedByUserIdAndIsDeletedFalse(
                        supplementId,
                        userId
                )
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "supplement.not.found",
                                supplementId
                        )
                );
    }

    private Routine findMyActiveRoutine(
            Long userId,
            Long routineId
    ) {
        return routineRepository.findByIdAndUserIdAndIsDeletedFalse(
                        routineId,
                        userId
                )
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "routine.not.found",
                                routineId
                        )
                );
    }
}