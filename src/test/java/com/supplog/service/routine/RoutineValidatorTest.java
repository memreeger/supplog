package com.supplog.service.routine;

import com.supplog.enums.DayOfWeek;
import com.supplog.enums.DurationType;
import com.supplog.enums.Frequency;
import com.supplog.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/*
Test 3 aşamadan oluşur:

1- ARRANGE -> Test için gerekli verileri hazırla
2- ACT     -> Test etmek istediğin kodu çalıştır
3- ASSERT  -> Sonucu kontrol et

Temel kural:
Bir testte sadece test etmek istediğimiz koşulu bozuyoruz.
Diğer parametreleri geçerli bırakıyoruz.
*/
class RoutineValidatorTest {

    private RoutineValidator routineValidator;

    @BeforeEach
    void setUp() {
        routineValidator = new RoutineValidator();
    }


    // =========================
    // DAILY
    // =========================

    @Test
    void shouldAcceptValidDailySchedule() {

        assertDoesNotThrow(() ->
                routineValidator.validateSchedule(
                        Frequency.DAILY,
                        null,
                        null,
                        LocalTime.of(8, 30)
                )
        );
    }

    @Test
    void shouldThrowExceptionWhenDailyScheduleHasNoTime() {

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> routineValidator.validateSchedule(
                        Frequency.DAILY,
                        null,
                        null,
                        null
                )
        );

        assertEquals(
                "routine.time.required",
                exception.getMessage()
        );
    }

    @Test
    void shouldThrowExceptionWhenDailyScheduleHasDaysOfWeek() {

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> routineValidator.validateSchedule(
                        Frequency.DAILY,
                        Set.of(DayOfWeek.MONDAY),
                        null,
                        LocalTime.of(8, 30)
                )
        );

        assertEquals(
                "routine.days.not.allowed",
                exception.getMessage()
        );
    }

    @Test
    void shouldThrowExceptionWhenDailyScheduleHasDayOfMonth() {

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> routineValidator.validateSchedule(
                        Frequency.DAILY,
                        null,
                        1,
                        LocalTime.of(8, 30)
                )
        );

        assertEquals(
                "routine.day.of.month.not.allowed",
                exception.getMessage()
        );
    }


    // =========================
    // WEEKLY
    // =========================

    @Test
    void shouldAcceptValidWeeklySchedule() {

        assertDoesNotThrow(() ->
                routineValidator.validateSchedule(
                        Frequency.WEEKLY,
                        Set.of(DayOfWeek.MONDAY),
                        null,
                        LocalTime.of(9, 30)
                )
        );
    }

    @Test
    void shouldThrowExceptionWhenWeeklyScheduleHasNoTime() {

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> routineValidator.validateSchedule(
                        Frequency.WEEKLY,
                        Set.of(DayOfWeek.MONDAY),
                        null,
                        null
                )
        );

        assertEquals(
                "routine.time.required",
                exception.getMessage()
        );
    }

    @Test
    void shouldThrowExceptionWhenWeeklyScheduleHasNoDay() {

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> routineValidator.validateSchedule(
                        Frequency.WEEKLY,
                        null,
                        null,
                        LocalTime.of(9, 30)
                )
        );

        assertEquals(
                "routine.weekly.single.day.required",
                exception.getMessage()
        );
    }

    @Test
    void shouldThrowExceptionWhenWeeklyScheduleHasMultipleDays() {

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> routineValidator.validateSchedule(
                        Frequency.WEEKLY,
                        Set.of(
                                DayOfWeek.MONDAY,
                                DayOfWeek.WEDNESDAY
                        ),
                        null,
                        LocalTime.of(9, 30)
                )
        );

        assertEquals(
                "routine.weekly.single.day.required",
                exception.getMessage()
        );
    }

    @Test
    void shouldThrowExceptionWhenWeeklyScheduleHasDayOfMonth() {

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> routineValidator.validateSchedule(
                        Frequency.WEEKLY,
                        Set.of(DayOfWeek.MONDAY),
                        1,
                        LocalTime.of(9, 30)
                )
        );

        assertEquals(
                "routine.day.of.month.not.allowed",
                exception.getMessage()
        );
    }


    // =========================
    // MONTHLY
    // =========================

    @Test
    void shouldAcceptValidMonthlySchedule() {

        assertDoesNotThrow(() ->
                routineValidator.validateSchedule(
                        Frequency.MONTHLY,
                        null,
                        1,
                        LocalTime.of(9, 30)
                )
        );
    }

    @Test
    void shouldThrowExceptionWhenMonthlyScheduleHasNoTime() {

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> routineValidator.validateSchedule(
                        Frequency.MONTHLY,
                        null,
                        1,
                        null
                )
        );

        assertEquals(
                "routine.time.required",
                exception.getMessage()
        );
    }

    @Test
    void shouldThrowExceptionWhenMonthlyScheduleHasNoDayOfMonth() {

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> routineValidator.validateSchedule(
                        Frequency.MONTHLY,
                        null,
                        null,
                        LocalTime.of(9, 30)
                )
        );

        assertEquals(
                "routine.day.of.month.required",
                exception.getMessage()
        );
    }

    @Test
    void shouldThrowExceptionWhenMonthlyScheduleHasDaysOfWeek() {

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> routineValidator.validateSchedule(
                        Frequency.MONTHLY,
                        Set.of(DayOfWeek.MONDAY),
                        1,
                        LocalTime.of(9, 30)
                )
        );

        assertEquals(
                "routine.days.not.allowed",
                exception.getMessage()
        );
    }


    // =========================
    // SPECIFIC DAYS
    // =========================

    @Test
    void shouldAcceptValidSpecificDaysSchedule() {

        assertDoesNotThrow(() ->
                routineValidator.validateSchedule(
                        Frequency.SPECIFIC_DAYS,
                        Set.of(
                                DayOfWeek.MONDAY,
                                DayOfWeek.FRIDAY
                        ),
                        null,
                        LocalTime.of(9, 30)
                )
        );
    }

    @Test
    void shouldThrowExceptionWhenSpecificDaysScheduleHasNoTime() {

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> routineValidator.validateSchedule(
                        Frequency.SPECIFIC_DAYS,
                        Set.of(DayOfWeek.MONDAY),
                        null,
                        null
                )
        );

        assertEquals(
                "routine.time.required",
                exception.getMessage()
        );
    }

    @Test
    void shouldThrowExceptionWhenSpecificDaysScheduleHasNoDays() {

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> routineValidator.validateSchedule(
                        Frequency.SPECIFIC_DAYS,
                        null,
                        null,
                        LocalTime.of(11, 0)
                )
        );

        assertEquals(
                "routine.days.required",
                exception.getMessage()
        );
    }

    @Test
    void shouldThrowExceptionWhenSpecificDaysScheduleHasDayOfMonth() {

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> routineValidator.validateSchedule(
                        Frequency.SPECIFIC_DAYS,
                        Set.of(DayOfWeek.MONDAY),
                        15,
                        LocalTime.of(15, 30)
                )
        );

        assertEquals(
                "routine.day.of.month.not.allowed",
                exception.getMessage()
        );
    }


    // =========================
    // AS NEEDED
    // =========================

    @Test
    void shouldAcceptValidAsNeededSchedule() {

        assertDoesNotThrow(() ->
                routineValidator.validateSchedule(
                        Frequency.AS_NEEDED,
                        null,
                        null,
                        null
                )
        );
    }

    @Test
    void shouldThrowExceptionWhenAsNeededScheduleHasTime() {

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> routineValidator.validateSchedule(
                        Frequency.AS_NEEDED,
                        null,
                        null,
                        LocalTime.of(15, 15)
                )
        );

        assertEquals(
                "routine.time.not.allowed",
                exception.getMessage()
        );
    }

    @Test
    void shouldThrowExceptionWhenAsNeededScheduleHasDaysOfWeek() {

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> routineValidator.validateSchedule(
                        Frequency.AS_NEEDED,
                        Set.of(DayOfWeek.FRIDAY),
                        null,
                        null
                )
        );

        assertEquals(
                "routine.days.not.allowed",
                exception.getMessage()
        );
    }

    @Test
    void shouldThrowExceptionWhenAsNeededScheduleHasDayOfMonth() {

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> routineValidator.validateSchedule(
                        Frequency.AS_NEEDED,
                        null,
                        1,
                        null
                )
        );

        assertEquals(
                "routine.day.of.month.not.allowed",
                exception.getMessage()
        );
    }


    // =========================
    // DURATION - LIFE TIME
    // =========================

    @Test
    void shouldAcceptLifeTimeDurationWithoutEndDate() {

        LocalDate startDate = LocalDate.of(2026, 8, 23);

        assertDoesNotThrow(() ->
                routineValidator.validateDuration(
                        DurationType.LIFE_TIME,
                        startDate,
                        null
                )
        );
    }

    @Test
    void shouldThrowExceptionWhenLifeTimeDurationHasEndDate() {

        LocalDate startDate = LocalDate.of(2026, 8, 23);
        LocalDate endDate = LocalDate.of(2027, 8, 23);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> routineValidator.validateDuration(
                        DurationType.LIFE_TIME,
                        startDate,
                        endDate
                )
        );

        assertEquals(
                "routine.end.date.not.allowed.for.life.time",
                exception.getMessage()
        );
    }


    // =========================
    // DURATION - UNTIL DATE
    // =========================

    @Test
    void shouldAcceptValidUntilDateDuration() {

        LocalDate startDate = LocalDate.of(2026, 8, 23);
        LocalDate endDate = LocalDate.of(2026, 12, 31);

        assertDoesNotThrow(() ->
                routineValidator.validateDuration(
                        DurationType.UNTIL_DATE,
                        startDate,
                        endDate
                )
        );
    }

    @Test
    void shouldAcceptUntilDateWhenEndDateEqualsStartDate() {

        LocalDate date = LocalDate.of(2026, 8, 23);

        assertDoesNotThrow(() ->
                routineValidator.validateDuration(
                        DurationType.UNTIL_DATE,
                        date,
                        date
                )
        );
    }

    @Test
    void shouldThrowExceptionWhenUntilDateHasNoEndDate() {

        LocalDate startDate = LocalDate.of(2026, 8, 23);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> routineValidator.validateDuration(
                        DurationType.UNTIL_DATE,
                        startDate,
                        null
                )
        );

        assertEquals(
                "routine.end.date.required",
                exception.getMessage()
        );
    }

    @Test
    void shouldThrowExceptionWhenEndDateIsBeforeStartDate() {

        LocalDate startDate = LocalDate.of(2026, 8, 23);
        LocalDate endDate = LocalDate.of(2026, 8, 22);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> routineValidator.validateDuration(
                        DurationType.UNTIL_DATE,
                        startDate,
                        endDate
                )
        );

        assertEquals(
                "routine.end.date.before.start.date",
                exception.getMessage()
        );
    }
}