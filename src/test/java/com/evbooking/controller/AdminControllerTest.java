package com.evbooking.controller;

import com.evbooking.config.SecurityConfig;
import com.evbooking.dto.BookingResponse;
import com.evbooking.dto.UserResponse;
import com.evbooking.security.CustomUserDetailsService;
import com.evbooking.security.SessionAuthEntryPoint;
import com.evbooking.service.BookingService;
import com.evbooking.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = "spring.session.store-type=none")
class AdminControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private UserService userService;
    @MockBean private BookingService bookingService;
    @MockBean private CustomUserDetailsService customUserDetailsService;
    @MockBean private SessionAuthEntryPoint sessionAuthEntryPoint;

    private BookingResponse sampleBooking() {
        return new BookingResponse(1L, 1L, "alice", 10L, "CCS2", 100L, "Station A",
                LocalDate.now().plusDays(1), LocalTime.of(10, 0), LocalTime.of(11, 0),
                "CONFIRMED", LocalDateTime.now());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanListUsers() throws Exception {
        when(userService.findAll()).thenReturn(List.of(new UserResponse(1L, "alice", "DRIVER")));

        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("alice"))
                .andExpect(jsonPath("$[0].role").value("DRIVER"));
    }

    @Test
    @WithMockUser(roles = "DRIVER")
    void driverBlockedFromAdminEndpoints() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin1", roles = "ADMIN")
    void adminCanCancelBooking() throws Exception {
        BookingResponse cancelled = new BookingResponse(1L, 1L, "alice", 10L, "CCS2", 100L, "Station A",
                LocalDate.now().plusDays(1), LocalTime.of(10, 0), LocalTime.of(11, 0),
                "CANCELLED", LocalDateTime.now());
        when(bookingService.cancelBooking("admin1", 1L, true)).thenReturn(cancelled);

        mockMvc.perform(delete("/api/admin/bookings/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

}
