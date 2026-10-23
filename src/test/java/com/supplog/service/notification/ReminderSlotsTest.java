package com.supplog.service.notification;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ReminderSlotsTest {

    private static final Instant SCHEDULED = Instant.parse("2026-10-23T08:00:00Z");

    private static Instant missedAfter(int graceMinutes) {
        return SCHEDULED.plus(Duration.ofMinutes(graceMinutes));
    }

    private static List<String> codes(List<ReminderSlot> slots) {
        return slots.stream().map(ReminderSlot::code).toList();
    }

    @Test
    void thirtyMinuteLeadAndGraceGiveTenSlotsBeforeAndEightAfter() {
        List<ReminderSlot> slots = ReminderSlots.slotsFor(SCHEDULED, missedAfter(30), 30);

        assertEquals(List.of(
                "PRE-30", "PRE-20", "PRE-10", "PRE-5", "PRE-4", "PRE-3", "PRE-2", "PRE-1",
                "LATE-0", "LATE-10", "LATE-20", "LATE-25", "LATE-26", "LATE-27", "LATE-28", "LATE-29"
        ), codes(slots));
    }

    @Test
    void shortLeadSkipsTheTenMinuteSteps() {
        assertEquals(List.of("PRE-10", "PRE-5", "PRE-4", "PRE-3", "PRE-2", "PRE-1"),
                codes(ReminderSlots.slotsFor(SCHEDULED, missedAfter(30), 10)).subList(0, 6));
    }

    @Test
    void longGraceKeepsTenMinuteStepsAndEndsWithTheLastFiveMinutes() {
        List<String> late = codes(ReminderSlots.slotsFor(SCHEDULED, missedAfter(120), 30)).stream()
                .filter(code -> code.startsWith("LATE")).toList();

        assertEquals("LATE-0", late.getFirst());
        assertTrue(late.contains("LATE-110"));
        assertFalse(late.contains("LATE-120"));
        assertEquals(List.of("LATE-115", "LATE-116", "LATE-117", "LATE-118", "LATE-119"),
                late.subList(late.size() - 5, late.size()));
    }

    @Test
    void slotsCarryTheMinutesLeftAndNeverOverlapOrRepeat() {
        for (int lead : new int[]{10, 20, 30, 60}) {
            for (int grace : new int[]{30, 60, 90, 120}) {
                List<ReminderSlot> slots = ReminderSlots.slotsFor(SCHEDULED, missedAfter(grace), lead);

                assertEquals(slots.size(), slots.stream().map(ReminderSlot::code).distinct().count(),
                        "duplicate codes for lead=" + lead + " grace=" + grace);
                assertEquals(slots.size(), slots.stream().map(ReminderSlot::at).distinct().count(),
                        "duplicate times for lead=" + lead + " grace=" + grace);
                slots.forEach(slot -> assertTrue(slot.at().isBefore(missedAfter(grace)), slot.code()));
            }
        }

        ReminderSlot pre = ReminderSlots.slotsFor(SCHEDULED, missedAfter(30), 30).getFirst();
        assertEquals(ReminderPhase.PRE, pre.phase());
        assertEquals(SCHEDULED.minus(Duration.ofMinutes(30)), pre.at());
        assertEquals(30, pre.minutes());

        ReminderSlot lastLate = ReminderSlots.slotsFor(SCHEDULED, missedAfter(30), 30).getLast();
        assertEquals("LATE-29", lastLate.code());
        assertEquals(1, lastLate.minutes());
    }

    @Test
    void currentSlotPicksTheLatestPastSlotWithinTolerance() {
        List<ReminderSlot> slots = ReminderSlots.slotsFor(SCHEDULED, missedAfter(30), 30);

        // 07:30:20 → PRE-30 yeni geçti.
        assertEquals("PRE-30", ReminderSlots.currentSlot(slots, Instant.parse("2026-10-23T07:30:20Z")).orElseThrow().code());
        // Zamanlayıcı bir tick kaçırdı: 07:31:30 hâlâ PRE-30'u yakalar (2 dk tolerans).
        assertEquals("PRE-30", ReminderSlots.currentSlot(slots, Instant.parse("2026-10-23T07:31:30Z")).orElseThrow().code());
        // 07:32:00'de PRE-30 çok eski ve sıradaki dilim (PRE-20) henüz gelmedi.
        assertEquals(Optional.empty(), ReminderSlots.currentSlot(slots, Instant.parse("2026-10-23T07:32:00Z")));
        // Dozun saati: LATE-0.
        assertEquals("LATE-0", ReminderSlots.currentSlot(slots, Instant.parse("2026-10-23T08:00:10Z")).orElseThrow().code());
        // Kaçırılmaya 3 dakika kala: LATE-27.
        assertEquals("LATE-27", ReminderSlots.currentSlot(slots, Instant.parse("2026-10-23T08:27:05Z")).orElseThrow().code());
    }

    @Test
    void noSlotBeforeTheFirstOneOrLongAfterTheLast() {
        List<ReminderSlot> slots = ReminderSlots.slotsFor(SCHEDULED, missedAfter(30), 30);

        assertTrue(ReminderSlots.currentSlot(slots, Instant.parse("2026-10-23T07:29:59Z")).isEmpty());
        assertTrue(ReminderSlots.currentSlot(slots, Instant.parse("2026-10-23T08:40:00Z")).isEmpty());
    }

    @Test
    void missedSlotIsAtMissedAt() {
        ReminderSlot slot = ReminderSlots.missedSlot(missedAfter(30));

        assertEquals(ReminderPhase.MISSED, slot.phase());
        assertEquals("MISSED", slot.code());
        assertEquals("MISSED", slot.kind());
        assertEquals("REMINDER", ReminderSlots.slotsFor(SCHEDULED, missedAfter(30), 30).getFirst().kind());
    }
}
