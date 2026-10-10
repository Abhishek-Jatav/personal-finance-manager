package com.example.finance.service;

import com.example.finance.dto.RegisterRequest;
import com.example.finance.dto.RegisterResponse;
import com.example.finance.entity.User;
import com.example.finance.exception.ConflictException;
import com.example.finance.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.SecurityContextRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock AuthenticationManager authenticationManager;
    @Mock SecurityContextRepository securityContextRepository;

    AuthService service;

    @BeforeEach
    void setUp() {
        service = new AuthService(userRepository, passwordEncoder, authenticationManager,
                securityContextRepository);
    }

    @Test
    void register_normalizesUsername_andStoresHashNotPlaintext() {
        when(userRepository.existsByEmail("alex@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("HASHED");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User saved = inv.getArgument(0);
            saved.setId(7L);
            return saved;
        });

        RegisterResponse response = service.register(
                new RegisterRequest("  Alex@Example.COM ", "password123", " Alex Example ", " +1234567890 "));

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertThat(saved.getValue().getEmail()).isEqualTo("alex@example.com");
        assertThat(saved.getValue().getPasswordHash()).isEqualTo("HASHED");
        assertThat(saved.getValue().getFullName()).isEqualTo("Alex Example");
        assertThat(saved.getValue().getPhoneNumber()).isEqualTo("+1234567890");
        assertThat(response.message()).isEqualTo("User registered successfully");
        assertThat(response.userId()).isEqualTo(7L);
    }

    @Test
    void register_duplicateUsername_throwsConflict() {
        when(userRepository.existsByEmail("alex@example.com")).thenReturn(true);

        assertThatThrownBy(() -> service.register(
                new RegisterRequest("alex@example.com", "password123", "Alex", "+1234567890")))
                .isInstanceOf(ConflictException.class);
        verify(userRepository, never()).save(any());
    }
}
