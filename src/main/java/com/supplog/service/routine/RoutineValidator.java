package com.supplog.service.routine;

import com.supplog.enums.DayOfWeek;
import com.supplog.enums.DurationType;
import com.supplog.enums.Frequency;
import com.supplog.exception.BusinessException;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

@Component
public class RoutineValidator {

    public void validateSchedule(
            Frequency frequency,
            Set<DayOfWeek> daysOfWeek,
            Integer dayOfMonth,
            LocalTime routineTime
    ) {

        switch (frequency) {

            case DAILY -> {
                if (routineTime == null) {
                    throw new BusinessException("routine.time.required");
                }

                if (daysOfWeek != null && !daysOfWeek.isEmpty()) {
                    throw new BusinessException("routine.days.not.allowed");
                }

                if (dayOfMonth != null) {
                    throw new BusinessException(
                            "routine.day.of.month.not.allowed"
                    );
                }
            }

            case WEEKLY -> {
                if (routineTime == null) {
                    throw new BusinessException("routine.time.required");
                }

                validateDays(frequency, daysOfWeek);

                if (dayOfMonth != null) {
                    throw new BusinessException(
                            "routine.day.of.month.not.allowed"
                    );
                }
            }

            case SPECIFIC_DAYS -> {
                if (routineTime == null) {
                    throw new BusinessException("routine.time.required");
                }

                validateDays(frequency, daysOfWeek);

                if (dayOfMonth != null) {
                    throw new BusinessException(
                            "routine.day.of.month.not.allowed"
                    );
                }
            }

            case MONTHLY -> {
                if (routineTime == null) {
                    throw new BusinessException("routine.time.required");
                }

                if (dayOfMonth == null) {
                    throw new BusinessException(
                            "routine.day.of.month.required"
                    );
                }

                if (dayOfMonth < 1 || dayOfMonth > 31) {
                    throw new BusinessException(
                            "validation.routine.dayOfMonth.invalid"
                    );
                }

                if (daysOfWeek != null && !daysOfWeek.isEmpty()) {
                    throw new BusinessException("routine.days.not.allowed");
                }
            }

            case AS_NEEDED -> {
                throw new BusinessException("routine.frequency.as.needed.not.supported");
            }
        }
    }

    public void validateDays(Frequency frequency, Set<DayOfWeek> daysOfWeek) {
        if (daysOfWeek == null || daysOfWeek.isEmpty()) {
            throw new BusinessException("routine.days.required");
        }

        if (frequency == Frequency.WEEKLY && daysOfWeek.size() != 1) {
            throw new BusinessException("routine.days.weekly.single.required");
        }

        if (frequency == Frequency.SPECIFIC_DAYS
                && (daysOfWeek.size() < 2 || daysOfWeek.size() > 6)) {
            throw new BusinessException("routine.days.specific.range.invalid");
        }
    }

    public void validateDuration(
            DurationType durationType,
            LocalDate startDate,
            LocalDate endDate
    ) {

        if (durationType == DurationType.LIFE_TIME) {

            if (endDate != null) {
                throw new BusinessException(
                        "routine.end.date.not.allowed.for.life.time"
                );
            }

            return;
        }

        if (durationType == DurationType.UNTIL_DATE) {

            if (endDate == null) {
                throw new BusinessException(
                        "routine.end.date.required"
                );
            }

            if (endDate.isBefore(startDate)) {
                throw new BusinessException(
                        "routine.end.date.before.start.date"
                );
            }
        }
    }
}
