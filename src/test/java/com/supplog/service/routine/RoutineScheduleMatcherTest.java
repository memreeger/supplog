package com.supplog.service.routine;

import com.supplog.entity.Routine;
import com.supplog.enums.DayOfWeek;
import com.supplog.enums.Frequency;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoutineScheduleMatcherTest {
    private final RoutineScheduleMatcher matcher = new RoutineScheduleMatcher();

    @Test
    void respectsDateRangeAndSelectedWeekday() {
        Routine routine = new Routine();
        routine.setFrequency(Frequency.WEEKLY);
        routine.setDaysOfWeek(Set.of(DayOfWeek.MONDAY));
        routine.setStartDate(LocalDate.of(2026, 9, 1));
        routine.setEndDate(LocalDate.of(2026, 9, 30));

        assertTrue(matcher.occursOn(routine, LocalDate.of(2026, 9, 7)));
        assertFalse(matcher.occursOn(routine, LocalDate.of(2026, 9, 8)));
        assertFalse(matcher.occursOn(routine, LocalDate.of(2026, 10, 5)));
    }
}
