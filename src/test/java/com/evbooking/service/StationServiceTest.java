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
    void enrichesStationsWithTodayBookingCount() {
        List<Object[]> counts = Collections.singletonList(new Object[]{1L, 3L});
        when(bookingRepository.countTodayGroupedByStation(any())).thenReturn(counts);
        when(stationRepository.findAllByOrderByNameAsc()).thenReturn(List.of(station));

        List<StationResponse> result = stationService.findAll();

        assertThat(result.get(0).todayBookingCount()).isEqualTo(3);
    }

    @Test
    void createsStation() {
        StationRequest req = new StationRequest("New Station", "456 Elm St", 51.0, 0.0, null);
        ChargingStation saved = new ChargingStation("New Station", "456 Elm St", 51.0, 0.0, null);
        saved.setId(2L);
        when(stationRepository.save(any())).thenReturn(saved);

        StationResponse response = stationService.create(req);

        assertThat(response.name()).isEqualTo("New Station");
        assertThat(response.address()).isEqualTo("456 Elm St");
    }

    @Test
    void deleteFailsWhenNotFound() {
        when(stationRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> stationService.delete(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
