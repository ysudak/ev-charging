package com.evbooking.service;

import com.evbooking.model.Booking;
import com.evbooking.model.ChargingStation;
import com.evbooking.model.Connector;
import com.evbooking.model.User;
import com.evbooking.repository.BookingRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingCompletionSchedulerTest {

    @Mock private BookingRepository bookingRepository;

    @InjectMocks private BookingCompletionScheduler scheduler;

    @Test
    void completePastBookings_marksPastBookingsAsCompleted() {
        User user = new User("driver", "pwd", User.Role.DRIVER);
        ChargingStation station = new ChargingStation("S", "addr", 0.0, 0.0, null);
        Connector connector = new Connector(station, "CCS2", 50);
        Booking past = new Booking(user, connector, LocalDate.now().minusDays(1),
                LocalTime.of(10, 0), LocalTime.of(11, 0));

        when(bookingRepository.findPastConfirmedBookings(any())).thenReturn(List.of(past));
        when(bookingRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        scheduler.completePastBookings();

        assertThat(past.getStatus()).isEqualTo(Booking.Status.COMPLETED);
        verify(bookingRepository).saveAll(List.of(past));
    }

    @Test
    void completePastBookings_noPastBookings_skips() {
        when(bookingRepository.findPastConfirmedBookings(any())).thenReturn(Collections.emptyList());

        scheduler.completePastBookings();

        verify(bookingRepository, never()).saveAll(any());
    }
}
