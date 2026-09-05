package com.supplog.service.routine;

import com.supplog.entity.Routine;
import com.supplog.enums.DayOfWeek;
import com.supplog.enums.Frequency;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Objects;

@Component
public class RoutineScheduleMatcher {

    public boolean occursOn(
            Routine routine,
            LocalDate date
    ) {

        if (date.isBefore(routine.getStartDate())) {
            return false;
        }

        if (routine.getEndDate() != null
                && date.isAfter(routine.getEndDate())) {
            return false;
        }

        return switch (routine.getFrequency()) {

            case DAILY -> true;

            case WEEKLY, SPECIFIC_DAYS -> {

                DayOfWeek dayOfWeek =
                        DayOfWeek.valueOf(
                                date.getDayOfWeek().name()
                        );

                yield routine.getDaysOfWeek()
                        .contains(dayOfWeek);
            }

            case MONTHLY ->
                    Objects.equals(
                            routine.getDayOfMonth(),
                            date.getDayOfMonth()
                    );

            case AS_NEEDED -> false;
        };
    }
}