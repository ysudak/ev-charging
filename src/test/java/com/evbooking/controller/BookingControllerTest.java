package com.evbooking.controller;

import com.evbooking.config.SecurityConfig;
import com.evbooking.dto.BookingRequest;
import com.evbooking.dto.BookingResponse;
import com.evbooking.dto.RescheduleRequest;
import com.evbooking.exception.BookingConflictException;
import com.evbooking.exception.ResourceNotFoundException;
import com.evbooking.security.CustomUserDetailsService;
import com.evbooking.security.SessionAuthEntryPoint;
import com.evbooking.service.BookingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BookingController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = "spring.session.store-type=none")
class BookingControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private BookingService bookingService;
    @MockBean private CustomUserDetailsService customUserDetailsService;
    @MockBean private SessionAuthEntryPoint sessionAuthEntryPoint;

    private BookingResponse sampleConfirmed() {
        return new BookingResponse(1L, 1L, "driver1", 10L, "CCS2", 100L, "Station A",
                LocalDate.now().plusDays(1), LocalTime.of(10, 0), LocalTime.of(11, 0),
                "CONFIRMED", LocalDateTime.now());
    }

    private BookingResponse sampleCancelled() {
        return new BookingResponse(1L, 1L, "driver1", 10L, "CCS2", 100L, "Station A",
                LocalDate.now().plusDays(1), LocalTime.of(10, 0), LocalTime.of(11, 0),
                "CANCELLED", LocalDateTime.now());
    }

    @Test
    @WithMockUser(username = "driver1", roles = "DRIVER")
    void createsBooking() throws Exception {
        BookingRequest req = new BookingRequest(10L, LocalDate.now().plusDays(1),
                LocalTime.of(10, 0), LocalTime.of(11, 0));
        when(bookingService.createBooking(eq("driver1"), any())).thenReturn(sampleConfirmed());

        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    @WithMockUser(username = "driver1", roles = "DRIVER")
    void returns409OnConflict() throws Exception {
        BookingRequest req = new BookingRequest(10L, LocalDate.now().plusDays(1),
                LocalTime.of(10, 0), LocalTime.of(11, 0));
        when(bookingService.createBooking(any(), any()))
                .thenThrow(new BookingConflictException("Slot already taken"));

        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @WithMockUser(username = "driver1", roles = "DRIVER")
    void returns400ForPastDate() throws Exception {
        BookingRequest req = new BookingRequest(10L, LocalDate.now().plusDays(1),
                LocalTime.of(10, 0), LocalTime.of(11, 0));
        when(bookingService.createBooking(any(), any()))
                .thenThrow(new IllegalArgumentException("Cannot book a slot in the past."));

        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "driver1", roles = "DRIVER")
    void returns404WhenNotFound() throws Exception {
        when(bookingService.findById(99L))
                .thenThrow(new ResourceNotFoundException("Booking not found: 99"));

        mockMvc.perform(get("/api/bookings/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @WithMockUser(username = "driver1", roles = "DRIVER")
    void driverCanCancelOwnBooking() throws Exception {
        when(bookingService.cancelBooking("driver1", 1L, false)).thenReturn(sampleCancelled());

        mockMvc.perform(delete("/api/bookings/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    @WithMockUser(username = "driver1", roles = "DRIVER")
    void returns409OnRescheduleConflict() throws Exception {
        RescheduleRequest req = new RescheduleRequest(LocalDate.now().plusDays(2),
                LocalTime.of(14, 0), LocalTime.of(15, 0));
        when(bookingService.rescheduleBooking(any(), any(), any()))
                .thenThrow(new BookingConflictException("New slot taken"));

        mockMvc.perform(put("/api/bookings/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict());
    }
}
