package com.evbooking.service;

import com.evbooking.dto.StationRequest;
import com.evbooking.dto.StationResponse;
import com.evbooking.exception.ResourceNotFoundException;
import com.evbooking.model.ChargingStation;
import com.evbooking.repository.BookingRepository;
import com.evbooking.repository.ChargingStationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StationServiceTest {

    @Mock private ChargingStationRepository stationRepository;
    @Mock private BookingRepository bookingRepository;

    @InjectMocks private StationService stationService;

    private ChargingStation station;

    @BeforeEach
    void setUp() {
        station = new ChargingStation("Station A", "123 Main St", 40.0, -74.0, "desc");
        station.setId(1L);
    }

    @Test
    void findAll_returnsMappedStations() {
        when(bookingRepository.countTodayGroupedByStation(any())).thenReturn(Collections.emptyList());
        when(stationRepository.findAllByOrderByNameAsc()).thenReturn(List.of(station));

        List<StationResponse> result = stationService.findAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("Station A");
        assertThat(result.get(0).todayBookingCount()).isZero();
    }

    @Test
    void findAll_enrichesTodayBookingCount() {
        List<Object[]> counts = Collections.singletonList(new Object[]{1L, 3L});
        when(bookingRepository.countTodayGroupedByStation(any())).thenReturn(counts);
        when(stationRepository.findAllByOrderByNameAsc()).thenReturn(List.of(station));

        List<StationResponse> result = stationService.findAll();

        assertThat(result.get(0).todayBookingCount()).isEqualTo(3);
    }

    @Test
    void findById_found() {
        when(stationRepository.findById(1L)).thenReturn(Optional.of(station));

        StationResponse response = stationService.findById(1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Station A");
    }

    @Test
    void findById_notFound_throws() {
        when(stationRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> stationService.findById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Station not found");
    }

    @Test
    void getEntity_notFound_throws() {
        when(stationRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> stationService.getEntity(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void create_success() {
        StationRequest req = new StationRequest("New Station", "456 Elm St", 51.0, 0.0, null);
        ChargingStation saved = new ChargingStation("New Station", "456 Elm St", 51.0, 0.0, null);
        saved.setId(2L);
        when(stationRepository.save(any())).thenReturn(saved);

        StationResponse response = stationService.create(req);

        assertThat(response.name()).isEqualTo("New Station");
        assertThat(response.address()).isEqualTo("456 Elm St");
    }

    @Test
    void update_success() {
        StationRequest req = new StationRequest("Updated Name", "789 Oak Ave", 52.0, 1.0, "updated");
        when(stationRepository.findById(1L)).thenReturn(Optional.of(station));
        when(stationRepository.save(station)).thenReturn(station);

        StationResponse response = stationService.update(1L, req);

        assertThat(response.name()).isEqualTo("Updated Name");
        assertThat(response.address()).isEqualTo("789 Oak Ave");
    }

    @Test
    void update_notFound_throws() {
        when(stationRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> stationService.update(99L, new StationRequest("X", "Y", 0.0, 0.0, null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void delete_success() {
        when(stationRepository.findById(1L)).thenReturn(Optional.of(station));

        stationService.delete(1L);

        verify(stationRepository).delete(station);
    }

    @Test
    void delete_notFound_throws() {
        when(stationRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> stationService.delete(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
