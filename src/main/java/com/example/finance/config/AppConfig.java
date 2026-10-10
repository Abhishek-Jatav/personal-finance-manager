package com.example.finance.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

/** General application beans, such as the business clock. */
@Configuration
public class AppConfig {

    /** All "today" logic uses this clock and the configured business timezone, so tests can fix the date. */
    @Bean
    public Clock clock(@Value("${app.timezone:Asia/Kolkata}") String timezone) {
        return Clock.system(ZoneId.of(timezone));
    }
}
