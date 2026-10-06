package com.supplog.enums;

import lombok.Getter;

@Getter
public enum MissedGracePeriod {
    THIRTY_MINUTES(30),
    SIXTY_MINUTES(60),
    NINETY_MINUTES(90),
    TWO_HOURS(120);

    private final int minutes;

    MissedGracePeriod(int minutes) {
        this.minutes = minutes;
    }
}
