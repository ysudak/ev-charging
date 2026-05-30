package com.evbooking.service;

import com.evbooking.dto.RegisterRequest;
import com.evbooking.dto.UserResponse;
import com.evbooking.exception.ResourceNotFoundException;
import com.evbooking.model.User;
import com.evbooking.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks private UserService userService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User("alice", "encoded", User.Role.DRIVER);
        user.setId(1L);
    }

    @Test
    void registersDriver() {
        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("encoded");
        when(userRepository.save(any())).thenReturn(user);

        UserResponse response = userService.register(new RegisterRequest("alice", "secret123"));

        assertThat(response.username()).isEqualTo("alice");
        assertThat(response.role()).isEqualTo("DRIVER");
        verify(passwordEncoder).encode("secret123");
    }

    @Test
    void rejectsDuplicateUsername() {
        when(userRepository.existsByUsername("alice")).thenReturn(true);

        assertThatThrownBy(() -> userService.register(new RegisterRequest("alice", "secret123")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Username already taken");
    }

    @Test
    void throwsWhenUsernameNotFound() {
        when(userRepository.findByUsername("nobody")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getEntityByUsername("nobody"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found");
    }
}
