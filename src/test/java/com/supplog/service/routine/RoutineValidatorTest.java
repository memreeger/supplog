package com.supplog.service.routine;

import com.supplog.enums.DayOfWeek;
import com.supplog.enums.Frequency;
import com.supplog.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RoutineValidatorTest {
    private final RoutineValidator validator = new RoutineValidator();

    @Test
    void weeklyRequiresExactlyOneDay() {
        assertDoesNotThrow(() -> validator.validateSchedule(
                Frequency.WEEKLY, Set.of(DayOfWeek.MONDAY), null, LocalTime.NOON));
        assertThrows(BusinessException.class, () -> validator.validateSchedule(
                Frequency.WEEKLY, Set.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY),
                null, LocalTime.NOON));
    }

    @Test
    void specificDaysRequiresTwoToSixDays() {
        assertDoesNotThrow(() -> validator.validateSchedule(
                Frequency.SPECIFIC_DAYS,
                Set.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY), null, LocalTime.NOON));
        assertThrows(BusinessException.class, () -> validator.validateSchedule(
                Frequency.SPECIFIC_DAYS, Set.of(DayOfWeek.MONDAY), null, LocalTime.NOON));
    }
}
