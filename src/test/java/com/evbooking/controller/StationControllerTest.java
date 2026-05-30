package com.evbooking.controller;

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
    void listAll_noAuthRequired_returns200() throws Exception {
        when(stationService.findAll()).thenReturn(List.of(sample()));

        mockMvc.perform(get("/api/stations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Station A"))
                .andExpect(jsonPath("$[0].connectorCount").value(2));
    }

    @Test
    void getOne_found_returns200() throws Exception {
        when(stationService.findById(1L)).thenReturn(sample());

        mockMvc.perform(get("/api/stations/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.address").value("123 Main St"));
    }

    @Test
    void getOne_notFound_returns404() throws Exception {
        when(stationService.findById(99L))
                .thenThrow(new ResourceNotFoundException("Station not found: 99"));

        mockMvc.perform(get("/api/stations/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Station not found: 99"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_asAdmin_returns201() throws Exception {
        StationRequest req = new StationRequest("New Station", "456 Elm St", 51.0, 0.0, null);
        when(stationService.create(any())).thenReturn(sample());

        mockMvc.perform(post("/api/stations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = "DRIVER")
    void create_asDriver_returns403() throws Exception {
        StationRequest req = new StationRequest("New Station", "456 Elm St", 51.0, 0.0, null);

        mockMvc.perform(post("/api/stations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_blankName_returns400() throws Exception {
        StationRequest req = new StationRequest("", "456 Elm St", 51.0, 0.0, null);

        mockMvc.perform(post("/api/stations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void update_asAdmin_returns200() throws Exception {
        StationRequest req = new StationRequest("Updated Name", "789 Oak Ave", 52.0, 1.0, null);
        when(stationService.update(eq(1L), any())).thenReturn(sample());

        mockMvc.perform(put("/api/stations/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "DRIVER")
    void update_asDriver_returns403() throws Exception {
        StationRequest req = new StationRequest("Updated", "789 Oak Ave", 52.0, 1.0, null);

        mockMvc.perform(put("/api/stations/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void delete_asAdmin_returns204() throws Exception {
        doNothing().when(stationService).delete(1L);

        mockMvc.perform(delete("/api/stations/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = "DRIVER")
    void delete_asDriver_returns403() throws Exception {
        mockMvc.perform(delete("/api/stations/1"))
                .andExpect(status().isForbidden());
    }
}
