package com.supplog.util;

import com.supplog.entity.User;
import com.supplog.exception.BusinessException;
import org.springframework.stereotype.Component;

import java.time.DateTimeException;
import java.time.ZoneId;

@Component
public class TimeZoneResolver {

    public String normalize(String timeZone) {

        if (timeZone == null || timeZone.isBlank()) {
            throw new BusinessException(
                    "user.timezone.invalid"
            );
        }

        try {
            return ZoneId
                    .of(timeZone.trim())
                    .getId();

        } catch (DateTimeException exception) {

            throw new BusinessException(
                    "user.timezone.invalid"
            );
        }
    }

    public ZoneId resolve(User user) {

        return resolve(
                user.getTimeZone()
        );
    }

    public ZoneId resolve(String timeZone) {

        try {
            return ZoneId.of(
                    timeZone.trim()
            );

        } catch (DateTimeException
                 | NullPointerException exception) {

            throw new BusinessException(
                    "user.timezone.invalid"
            );
        }
    }
}