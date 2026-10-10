package com.example.finance.service;

import com.example.finance.dto.LoginRequest;
import com.example.finance.dto.MessageResponse;
import com.example.finance.dto.RegisterRequest;
import com.example.finance.dto.RegisterResponse;
import com.example.finance.entity.User;
import com.example.finance.exception.ConflictException;
import com.example.finance.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Registration, login and logout. Login state lives in the HTTP session (the session cookie). */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       SecurityContextRepository securityContextRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
    }

    /**
     * Creates a new account. The username (an email) is trimmed and lower-cased, and the password is stored as a hash.
     *
     * @throws ConflictException if the username is already registered
     */
    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.username());
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("An account with this username already exists");
        }
        User user = new User();
        user.setFullName(request.fullName().trim());
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setPhoneNumber(request.phoneNumber().trim());
        try {
            User saved = userRepository.save(user);
            return new RegisterResponse("User registered successfully", saved.getId());
        } catch (DataIntegrityViolationException ex) {
            throw new ConflictException("An account with this username already exists");
        }
    }

    /**
     * Checks the credentials and stores the login in the HTTP session.
     * A wrong username or password results in a 401 response.
     */
    public MessageResponse login(LoginRequest request, HttpServletRequest httpRequest,
                                 HttpServletResponse httpResponse) {
        String email = normalizeEmail(request.username());
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, request.password()));

        if (httpRequest.getSession(false) != null) {
            httpRequest.changeSessionId(); // protects against session fixation
        }
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, httpRequest, httpResponse);
        return new MessageResponse("Login successful");
    }

    /** Invalidates the current session. */
    public MessageResponse logout(HttpServletRequest httpRequest) {
        HttpSession session = httpRequest.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
        return new MessageResponse("Logout successful");
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }
}
