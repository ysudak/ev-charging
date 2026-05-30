package com.evbooking.controller;

import com.evbooking.config.SecurityConfig;
import com.evbooking.dto.BookedTimeResponse;
import com.evbooking.dto.ConnectorRequest;
import com.evbooking.dto.ConnectorResponse;
import com.evbooking.exception.ResourceNotFoundException;
import com.evbooking.security.CustomUserDetailsService;
import com.evbooking.security.SessionAuthEntryPoint;
import com.evbooking.service.BookingService;
import com.evbooking.service.ConnectorService;
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

import java.time.LocalTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ConnectorController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = "spring.session.store-type=none")
class ConnectorControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private ConnectorService connectorService;
    @MockBean private BookingService bookingService;
    @MockBean private CustomUserDetailsService customUserDetailsService;
    @MockBean private SessionAuthEntryPoint sessionAuthEntryPoint;

    private ConnectorResponse sample() {
        return new ConnectorResponse(10L, 1L, "CCS2", 50);
    }

    @Test
    void listsConnectorsWithoutLogin() throws Exception {
        when(connectorService.findByStation(1L)).thenReturn(List.of(sample()));

        mockMvc.perform(get("/api/stations/1/connectors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].connectorType").value("CCS2"))
                .andExpect(jsonPath("$[0].powerKw").value(50));
    }

    @Test
    void returns404WhenNotFound() throws Exception {
        when(connectorService.findById(99L))
                .thenThrow(new ResourceNotFoundException("Connector not found: 99"));

        mockMvc.perform(get("/api/connectors/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanAddConnector() throws Exception {
        ConnectorRequest req = new ConnectorRequest("Type2", 22);
        when(connectorService.create(eq(1L), any())).thenReturn(sample());

        mockMvc.perform(post("/api/stations/1/connectors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = "DRIVER")
    void driverCannotAddConnector() throws Exception {
        mockMvc.perform(post("/api/stations/1/connectors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ConnectorRequest("Type2", 22))))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanDeleteConnector() throws Exception {
        doNothing().when(connectorService).delete(10L);

        mockMvc.perform(delete("/api/connectors/10"))
                .andExpect(status().isNoContent());
    }

    @Test
    void returnsBookedTimesWithoutLogin() throws Exception {
        when(bookingService.getBookedTimes(eq(10L), any()))
                .thenReturn(List.of(new BookedTimeResponse(LocalTime.of(10, 0), LocalTime.of(11, 0))));

        mockMvc.perform(get("/api/connectors/10/booked-times")
                        .param("date", "2026-06-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].startTime").value("10:00:00"))
                .andExpect(jsonPath("$[0].endTime").value("11:00:00"));
    }
}
