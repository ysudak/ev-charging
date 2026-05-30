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
    void findByStation_returnsList() {
        when(connectorRepository.findByStationId(1L)).thenReturn(List.of(connector));

        List<ConnectorResponse> result = connectorService.findByStation(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).connectorType()).isEqualTo("CCS2");
        assertThat(result.get(0).powerKw()).isEqualTo(50);
    }

    @Test
    void findById_found() {
        when(connectorRepository.findById(10L)).thenReturn(Optional.of(connector));

        ConnectorResponse response = connectorService.findById(10L);

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.stationId()).isEqualTo(1L);
    }

    @Test
    void findById_notFound_throws() {
        when(connectorRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> connectorService.findById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Connector not found");
    }

    @Test
    void getEntity_notFound_throws() {
        when(connectorRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> connectorService.getEntity(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void create_success() {
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
    void update_success() {
        ConnectorRequest req = new ConnectorRequest("Type2", 22);
        when(connectorRepository.findById(10L)).thenReturn(Optional.of(connector));
        when(connectorRepository.save(connector)).thenReturn(connector);

        ConnectorResponse response = connectorService.update(10L, req);

        assertThat(response.connectorType()).isEqualTo("Type2");
        assertThat(response.powerKw()).isEqualTo(22);
    }

    @Test
    void delete_success() {
        when(connectorRepository.findById(10L)).thenReturn(Optional.of(connector));

        connectorService.delete(10L);

        verify(connectorRepository).delete(connector);
    }

    @Test
    void delete_notFound_throws() {
        when(connectorRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> connectorService.delete(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
