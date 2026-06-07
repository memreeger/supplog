package com.supplog.service.routine.impl;

import com.supplog.dto.routine.BaseRoutineDto;
import com.supplog.dto.routine.CreateRoutineRequestDto;
import com.supplog.dto.routine.ResponseRoutineDto;
import com.supplog.entity.Routine;
import com.supplog.entity.Supplement;
import com.supplog.entity.User;
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
    private final ModelMapper mapper;
    private final UserRepository userRepository;
    private final SupplementRepository supplementRepository;

    public RoutineServiceImpl(RoutineRepository routineRepository, ModelMapper mapper, UserRepository userRepository, SupplementRepository supplementRepository) {

        this.routineRepository = routineRepository;

        this.mapper = mapper;
        this.userRepository = userRepository;
        this.supplementRepository = supplementRepository;
    }


    @Override
    public void addRoutine(CreateRoutineRequestDto routineRequestDto) {
        Routine routine = new Routine();

        User user = userRepository.findById(routineRequestDto.getUserId()).orElseThrow(() -> new IllegalArgumentException("user id sorunu"));
        Supplement supplement = supplementRepository.findById(routineRequestDto.getSupplementId()).orElseThrow(() -> new IllegalArgumentException("supp id sorunu"));

        routine.setUser(user);
        routine.setSupplement(supplement);

        routine.setDayName(routineRequestDto.getDayName());
        routine.setHour(routineRequestDto.getHour());
        routine.setMinute(routineRequestDto.getMinute());
        routine.setPeriod(routineRequestDto.getPeriod());


        routineRepository.save(routine);
    }

    @Override
    public List<ResponseRoutineDto> getAllRoutinesByUserId(Long userId) {
        List<Routine> routines = routineRepository.findAllByUserId(userId);
        List<ResponseRoutineDto> routineDtos = routines.stream().map(r -> mapper.map(r, ResponseRoutineDto.class)).toList();

        return routineDtos;
    }
}
