package com.example.finance.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

/** Fixes "today" to 2026-10-10 (Asia/Kolkata) so date rules are tested deterministically. */
@TestConfiguration
public class TestClockConfig {

    public static final Instant NOW = Instant.parse("2026-10-10T06:30:00Z");
    public static final ZoneId ZONE = ZoneId.of("Asia/Kolkata");

    @Bean
    @Primary
    public Clock testClock() {
        return Clock.fixed(NOW, ZONE);
    }
}
