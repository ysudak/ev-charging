package com.evbooking.service;

import com.evbooking.dto.ConnectorRequest;
import com.evbooking.dto.ConnectorResponse;
import com.evbooking.exception.ResourceNotFoundException;
import com.evbooking.model.ChargingStation;
import com.evbooking.model.Connector;
import com.evbooking.repository.ConnectorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConnectorServiceTest {

    @Mock private ConnectorRepository connectorRepository;
    @Mock private StationService stationService;

    @InjectMocks private ConnectorService connectorService;

    private ChargingStation station;
    private Connector connector;

    @BeforeEach
    void setUp() {
        station = new ChargingStation("Station A", "123 Main St", 40.0, -74.0, "desc");
        station.setId(1L);

        connector = new Connector(station, "CCS2", 50);
        connector.setId(10L);
    }

    @Test
    void createsConnector() {
        ConnectorRequest req = new ConnectorRequest("CHAdeMO", 100);
        Connector saved = new Connector(station, "CHAdeMO", 100);
        saved.setId(20L);
        when(stationService.getEntity(1L)).thenReturn(station);
        when(connectorRepository.save(any())).thenReturn(saved);

        ConnectorResponse response = connectorService.create(1L, req);

        assertThat(response.connectorType()).isEqualTo("CHAdeMO");
        assertThat(response.powerKw()).isEqualTo(100);
        assertThat(response.stationId()).isEqualTo(1L);
    }

    @Test
    void deleteFailsWhenNotFound() {
        when(connectorRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> connectorService.delete(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
