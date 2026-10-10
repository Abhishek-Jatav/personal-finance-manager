package com.example.finance.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Size(max = 100) String fullName,
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Size(min = 8, max = 72, message = "size must be between 8 and 72") String password,
        @Pattern(regexp = "^\\+?[0-9 ()\\-]{6,20}$", message = "must be a valid phone number") String phoneNumber) {
}
