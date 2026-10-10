package com.example.finance.dto;

import com.example.finance.entity.User;

public record UserResponse(Long id, String fullName, String email, String phoneNumber) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getFullName(), user.getEmail(), user.getPhoneNumber());
    }
}
