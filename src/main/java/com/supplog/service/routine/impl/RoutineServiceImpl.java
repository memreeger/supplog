package com.supplog.service.routine.impl;

import com.supplog.dto.routine.*;
import com.supplog.entity.Routine;
import com.supplog.entity.Supplement;
import com.supplog.entity.User;
import com.supplog.enums.DayOfWeek;
import com.supplog.enums.DurationType;
import com.supplog.enums.Frequency;
import com.supplog.enums.MissedGracePeriod;
import com.supplog.exception.BusinessException;
import com.supplog.exception.ResourceNotFoundException;
import com.supplog.repository.RoutineRepository;
import com.supplog.repository.SupplementRepository;
import com.supplog.service.routine.RoutineService;
import com.supplog.service.routine.RoutineValidator;
import com.supplog.service.routineExecution.RoutineExecutionService;
import com.supplog.service.support.SupportService;
import com.supplog.service.user.ActiveUserService;
import com.supplog.util.TimeZoneResolver;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class RoutineServiceImpl implements RoutineService {
    private final RoutineRepository routineRepository;
    private final SupplementRepository supplementRepository;
    private final ActiveUserService activeUserService;
    private final ModelMapper mapper;
    private final RoutineValidator routineValidator;
    private final SupportService supportService;
    private final TimeZoneResolver timeZoneResolver;
    private final RoutineExecutionService routineExecutionService;

    public RoutineServiceImpl(RoutineRepository routineRepository, ModelMapper mapper, ActiveUserService activeUserService, SupplementRepository supplementRepository, RoutineValidator routineValidator, SupportService supportService, TimeZoneResolver timeZoneResolver, RoutineExecutionService routineExecutionService) {

        this.routineRepository = routineRepository;
        this.supplementRepository = supplementRepository;
        this.activeUserService = activeUserService;
        this.mapper = mapper;
        this.routineValidator = routineValidator;
        this.supportService = supportService;
        this.timeZoneResolver = timeZoneResolver;
        this.routineExecutionService = routineExecutionService;
    }


    @Override
    @Transactional
    public void addRoutine(Long userId, CreateRoutineRequestDto routineRequestDto) {

        User user = findActiveUser(userId);

        LocalDate startDate =
                resolveCreateStartDate(
                        routineRequestDto,
                        user
                );

        routineValidator.validateSchedule(
                routineRequestDto.getFrequency(),
                routineRequestDto.getDaysOfWeek(),
                routineRequestDto.getDayOfMonth(),
                routineRequestDto.getRoutineTime()
        );

        routineValidator.validateDuration(
                routineRequestDto.getDurationType(),
                startDate,
                routineRequestDto.getEndDate()
        );


        Supplement supplement = findMyActiveSupplement(
                userId,
                routineRequestDto.getSupplementId()
        );

        Routine routine = new Routine();

        routine.setUser(user);
        routine.setSupplement(supplement);

        routine.setFrequency(routineRequestDto.getFrequency());
        routine.setDurationType(routineRequestDto.getDurationType());

        routine.setDaysOfWeek(
                routineRequestDto.getDaysOfWeek() != null
                        ? new HashSet<>(routineRequestDto.getDaysOfWeek())
                        : new HashSet<>()
        );

        routine.setDayOfMonth(routineRequestDto.getDayOfMonth());
        routine.setRoutineTime(routineRequestDto.getRoutineTime());

        routine.setStartDate(startDate);
        routine.setEndDate(routineRequestDto.getEndDate());
        routine.setMissedGracePeriod(
                routineRequestDto.getMissedGracePeriod() != null
                        ? routineRequestDto.getMissedGracePeriod()
                        : MissedGracePeriod.THIRTY_MINUTES
        );

        routine.setDeleted(false);

        routineRepository.save(routine);
    }

    @Override
    public List<RoutineResponseDto> getMyRoutines(Long userId) {
        activeUserService.requireActiveUser(userId);

        List<Routine> allRoutines = routineRepository
                .findAllByUserIdAndIsDeletedFalseAndSupplementIsDeletedFalse(userId);
        List<RoutineResponseDto> allDtoList = new ArrayList<>();

        for (Routine routine : allRoutines) {
            allDtoList.add(toRoutineResponseDto(routine));
        }
        return allDtoList;
    }

    @Override
    public RoutineResponseDto getMyRoutineById(Long userId, Long routineId) {
        Routine routine = findMyActiveRoutine(userId, routineId);
        return toRoutineResponseDto(routine);

    }

    @Override
    @Transactional
    public void updateRoutine(Long userId, Long routineId, UpdateRoutineRequestDto requestDto) {
        Routine routine = findMyActiveRoutineForUpdate(userId, routineId);
        LocalDate startDate = requestDto.getStartDate() != null
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

    }


    @Override
    @Transactional
    public void deleteRoutine(Long userId, Long routineId) {

        Routine routine = findMyActiveRoutineForUpdate(userId, routineId);
        supportService.handleRoutineSoftDelete(routine.getId());
        routineExecutionService.handleRoutineSoftDelete(routine);
        routine.setDeleted(true);
    }

    @Override
    @Transactional
    public void updateRoutineTime(Long userId, Long routineId, UpdateRoutineTimeRequestDto requestDto) {


        Routine routine = findMyActiveRoutineForUpdate(userId, routineId);

        if (routine.getFrequency() == Frequency.AS_NEEDED) {
            throw new BusinessException("routine.time.not.allowed");
        }

        routine.setRoutineTime(requestDto.getRoutineTime());

        routineExecutionService.synchronizePendingAfterRoutineUpdate(routine);

    }

    @Override
    @Transactional
    public void updateRoutineDays(Long userId, Long routineId, UpdateRoutineDaysRequestDto requestDto) {

        Routine routine = findMyActiveRoutineForUpdate(userId, routineId);

        if (routine.getFrequency() != Frequency.WEEKLY
                && routine.getFrequency() != Frequency.SPECIFIC_DAYS) {

            throw new BusinessException("routine.days.not.allowed");
        }

        routineValidator.validateDays(
                routine.getFrequency(),
                requestDto.getDaysOfWeek()
        );

        updateRoutineDaysCollection(
                routine,
                requestDto.getDaysOfWeek()
        );

        routineExecutionService.synchronizePendingAfterRoutineUpdate(routine);
    }

    @Override
    @Transactional
    public void updateRoutineFrequency(
            Long userId,
            Long routineId,
            UpdateRoutineFrequencyRequestDto requestDto
    ) {

        Routine routine = findMyActiveRoutineForUpdate(userId, routineId);

        routineValidator.validateSchedule(
                requestDto.getFrequency(),
                requestDto.getDaysOfWeek(),
                requestDto.getDayOfMonth(),
                requestDto.getRoutineTime()
        );

        routine.setFrequency(requestDto.getFrequency());

        updateRoutineDaysCollection(
                routine,
                requestDto.getDaysOfWeek()
        );

        routine.setDayOfMonth(requestDto.getDayOfMonth());
        routine.setRoutineTime(requestDto.getRoutineTime());

        routineExecutionService.synchronizePendingAfterRoutineUpdate(routine);
    }

    @Override
    @Transactional
    public void updateRoutineDayOfMonth(
            Long userId,
            Long routineId,
            UpdateRoutineDayOfMonthRequestDto requestDto
    ) {

        Routine routine = findMyActiveRoutineForUpdate(userId, routineId);

        if (routine.getFrequency() != Frequency.MONTHLY) {
            throw new BusinessException(
                    "routine.day.of.month.not.allowed"
            );
        }

        routine.setDayOfMonth(requestDto.getDayOfMonth());

        routineExecutionService.synchronizePendingAfterRoutineUpdate(routine);
    }

    @Override
    @Transactional
    public void updateRoutineDuration(
            Long userId,
            Long routineId,
            UpdateRoutineDurationRequestDto requestDto
    ) {

        Routine routine = findMyActiveRoutineForUpdate(userId, routineId);

        LocalDate startDate = requestDto.getStartDate() != null
                ? requestDto.getStartDate()
                : routine.getStartDate();

        routineValidator.validateDuration(
                requestDto.getDurationType(),
                startDate,
                requestDto.getEndDate()
        );

        routine.setDurationType(requestDto.getDurationType());
        routine.setStartDate(startDate);
        routine.setEndDate(requestDto.getEndDate());

        routineExecutionService.synchronizePendingAfterRoutineUpdate(routine);
    }

    //Helper method
    private User findActiveUser(Long userId) {
        return activeUserService.getRequiredActiveUser(userId);
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
        activeUserService.requireActiveUser(userId);

        return routineRepository.findByIdAndUserIdAndIsDeletedFalseAndSupplementIsDeletedFalse(
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

    private Routine findMyActiveRoutineForUpdate(
            Long userId,
            Long routineId
    ) {
        activeUserService.requireActiveUser(userId);

        return routineRepository.findActiveByIdAndUserIdForUpdate(
                        routineId,
                        userId
                )
                .orElseThrow(() -> new ResourceNotFoundException(
                        "routine.not.found",
                        routineId
                ));
    }

    private LocalDate resolveCreateStartDate(
            CreateRoutineRequestDto requestDto,
            User user
    ) {

        if (requestDto.getStartDate() != null) {
            return requestDto.getStartDate();
        }

        return Instant.now()
                .atZone(
                        timeZoneResolver.resolve(user)
                )
                .toLocalDate();
    }


    private RoutineResponseDto toRoutineResponseDto(Routine routine) {

        RoutineResponseDto dto = mapper.map(
                routine,
                RoutineResponseDto.class
        );

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
