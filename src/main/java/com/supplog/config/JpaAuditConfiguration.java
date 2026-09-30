package com.supplog.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;

@Configuration
public class JpaAuditConfiguration {

    @Bean
    DateTimeProvider utcDateTimeProvider() {
        Clock utcClock = Clock.systemUTC();
        return () -> Optional.of(LocalDateTime.now(utcClock));
    }
}
