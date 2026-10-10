package com.example.finance.service;

import com.example.finance.entity.User;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

final class ServiceTestSupport {

    static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-10T06:30:00Z"), ZoneId.of("Asia/Kolkata"));
    static final LocalDate TODAY = LocalDate.of(2026, 10, 10);

    private ServiceTestSupport() {
    }

    static User user(long id) {
        User user = new User();
        user.setId(id);
        user.setEmail("user" + id + "@example.com");
        return user;
    }
}
