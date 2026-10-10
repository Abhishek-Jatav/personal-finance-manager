package com.example.finance.controller;

import com.example.finance.dto.LoginRequest;
import com.example.finance.dto.MessageResponse;
import com.example.finance.dto.RegisterRequest;
import com.example.finance.dto.RegisterResponse;
import com.example.finance.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Routes under /api/auth. */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /** POST /api/auth/register - creates an account (201). */
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public RegisterResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    /** POST /api/auth/login - starts a session and sets the session cookie (200). */
    @PostMapping("/login")
    public MessageResponse login(@Valid @RequestBody LoginRequest request,
                                 HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        return authService.login(request, httpRequest, httpResponse);
    }

    /** POST /api/auth/logout - ends the session (200, or 401 when not logged in). */
    @PostMapping("/logout")
    public MessageResponse logout(HttpServletRequest httpRequest) {
        return authService.logout(httpRequest);
    }
}
