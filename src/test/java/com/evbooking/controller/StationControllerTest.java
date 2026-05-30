package com.evbooking.controller;

import com.evbooking.config.SecurityConfig;
import com.evbooking.dto.StationRequest;
import com.evbooking.dto.StationResponse;
import com.evbooking.exception.ResourceNotFoundException;
import com.evbooking.security.CustomUserDetailsService;
import com.evbooking.security.SessionAuthEntryPoint;
import com.evbooking.service.StationService;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(StationController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = "spring.session.store-type=none")
class StationControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private StationService stationService;
    @MockBean private CustomUserDetailsService customUserDetailsService;
    @MockBean private SessionAuthEntryPoint sessionAuthEntryPoint;

    private StationResponse sample() {
        return new StationResponse(1L, "Station A", "123 Main St", 40.0, -74.0, "desc", 2, 0);
    }

    @Test
    void listsStationsWithoutLogin() throws Exception {
        when(stationService.findAll()).thenReturn(List.of(sample()));

        mockMvc.perform(get("/api/stations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Station A"))
                .andExpect(jsonPath("$[0].connectorCount").value(2));
    }

    @Test
    void returns404WhenNotFound() throws Exception {
        when(stationService.findById(99L))
                .thenThrow(new ResourceNotFoundException("Station not found: 99"));

        mockMvc.perform(get("/api/stations/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Station not found: 99"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanCreateStation() throws Exception {
        StationRequest req = new StationRequest("New Station", "456 Elm St", 51.0, 0.0, null);
        when(stationService.create(any())).thenReturn(sample());

        mockMvc.perform(post("/api/stations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = "DRIVER")
    void driverCannotCreateStation() throws Exception {
        StationRequest req = new StationRequest("New Station", "456 Elm St", 51.0, 0.0, null);

        mockMvc.perform(post("/api/stations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void returns400ForBlankName() throws Exception {
        StationRequest req = new StationRequest("", "456 Elm St", 51.0, 0.0, null);

        mockMvc.perform(post("/api/stations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanDeleteStation() throws Exception {
        doNothing().when(stationService).delete(1L);

        mockMvc.perform(delete("/api/stations/1"))
                .andExpect(status().isNoContent());
    }

}
