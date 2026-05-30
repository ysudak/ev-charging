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
    void register_success() {
        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("encoded");
        when(userRepository.save(any())).thenReturn(user);

        UserResponse response = userService.register(new RegisterRequest("alice", "secret123"));

        assertThat(response.username()).isEqualTo("alice");
        assertThat(response.role()).isEqualTo("DRIVER");
        verify(passwordEncoder).encode("secret123");
    }

    @Test
    void register_duplicateUsername_throws() {
        when(userRepository.existsByUsername("alice")).thenReturn(true);

        assertThatThrownBy(() -> userService.register(new RegisterRequest("alice", "secret123")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Username already taken");
    }

    @Test
    void findAll_returnsAllUsers() {
        when(userRepository.findAll()).thenReturn(List.of(user));

        List<UserResponse> result = userService.findAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).username()).isEqualTo("alice");
    }

    @Test
    void findById_found() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserResponse response = userService.findById(1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.username()).isEqualTo("alice");
    }

    @Test
    void findById_notFound_throws() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.findById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    void getEntityByUsername_found() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));

        User result = userService.getEntityByUsername("alice");

        assertThat(result.getUsername()).isEqualTo("alice");
        assertThat(result.getRole()).isEqualTo(User.Role.DRIVER);
    }

    @Test
    void getEntityByUsername_notFound_throws() {
        when(userRepository.findByUsername("nobody")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getEntityByUsername("nobody"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found");
    }
}
