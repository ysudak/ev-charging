package com.evbooking.service;

import com.evbooking.dto.BookingRequest;
import com.evbooking.dto.BookingResponse;
import com.evbooking.dto.RescheduleRequest;
import com.evbooking.exception.BookingConflictException;
import com.evbooking.exception.ResourceNotFoundException;
import com.evbooking.model.Booking;
import com.evbooking.model.ChargingStation;
import com.evbooking.model.Connector;
import com.evbooking.model.User;
import com.evbooking.repository.BookingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock private BookingRepository bookingRepository;
    @Mock private ConnectorService connectorService;
    @Mock private UserService userService;

    @InjectMocks private BookingService bookingService;

    private User driver;
    private User admin;
    private ChargingStation station;
    private Connector connector;
    private Booking booking;

    private static final LocalDate TOMORROW = LocalDate.now().plusDays(1);
    private static final LocalDate YESTERDAY = LocalDate.now().minusDays(1);

    @BeforeEach
    void setUp() {
        driver = new User("driver1", "encoded", User.Role.DRIVER);
        driver.setId(1L);

        admin = new User("admin1", "encoded", User.Role.ADMIN);
        admin.setId(2L);

        station = new ChargingStation("Station A", "123 Main St", 40.0, -74.0, "desc");
        station.setId(10L);

        connector = new Connector(station, "CCS2", 50);
        connector.setId(100L);

        booking = new Booking(driver, connector, TOMORROW, LocalTime.of(10, 0), LocalTime.of(11, 0));
        booking.setId(1000L);
    }

    @Test
    void createsBooking() {
        BookingRequest req = new BookingRequest(100L, TOMORROW, LocalTime.of(9, 0), LocalTime.of(10, 0));
        Booking saved = new Booking(driver, connector, TOMORROW, LocalTime.of(9, 0), LocalTime.of(10, 0));
        saved.setId(999L);

        when(userService.getEntityByUsername("driver1")).thenReturn(driver);
        when(connectorService.getEntity(100L)).thenReturn(connector);
        when(bookingRepository.findConflictingBookingsForLock(any(), any(), any(), any()))
                .thenReturn(Collections.emptyList());
        when(bookingRepository.findDriverOverlappingBookings(any(), any(), any(), any()))
                .thenReturn(Collections.emptyList());
        when(bookingRepository.save(any())).thenReturn(saved);

        BookingResponse response = bookingService.createBooking("driver1", req);

        assertThat(response.connectorId()).isEqualTo(100L);
        assertThat(response.startTime()).isEqualTo(LocalTime.of(9, 0));
        assertThat(response.status()).isEqualTo("CONFIRMED");
    }

    @Test
    void rejectsEndBeforeStart() {
        BookingRequest req = new BookingRequest(100L, TOMORROW, LocalTime.of(11, 0), LocalTime.of(10, 0));

        assertThatThrownBy(() -> bookingService.createBooking("driver1", req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("End time must be after start time");
    }

    @Test
    void rejectsPastDate() {
        BookingRequest req = new BookingRequest(100L, YESTERDAY, LocalTime.of(10, 0), LocalTime.of(11, 0));

        assertThatThrownBy(() -> bookingService.createBooking("driver1", req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("past");
    }

    @Test
    void adminCannotBook() {
        BookingRequest req = new BookingRequest(100L, TOMORROW, LocalTime.of(10, 0), LocalTime.of(11, 0));
        when(userService.getEntityByUsername("admin1")).thenReturn(admin);

        assertThatThrownBy(() -> bookingService.createBooking("admin1", req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Administrators cannot make reservations");
    }

    @Test
    void rejectsConnectorConflict() {
        BookingRequest req = new BookingRequest(100L, TOMORROW, LocalTime.of(10, 0), LocalTime.of(11, 0));
        when(userService.getEntityByUsername("driver1")).thenReturn(driver);
        when(connectorService.getEntity(100L)).thenReturn(connector);
        when(bookingRepository.findConflictingBookingsForLock(any(), any(), any(), any()))
                .thenReturn(List.of(booking));

        assertThatThrownBy(() -> bookingService.createBooking("driver1", req))
                .isInstanceOf(BookingConflictException.class);
    }

    @Test
    void rejectsDriverOverlap() {
        BookingRequest req = new BookingRequest(100L, TOMORROW, LocalTime.of(10, 0), LocalTime.of(11, 0));
        when(userService.getEntityByUsername("driver1")).thenReturn(driver);
        when(connectorService.getEntity(100L)).thenReturn(connector);
        when(bookingRepository.findConflictingBookingsForLock(any(), any(), any(), any()))
                .thenReturn(Collections.emptyList());
        when(bookingRepository.findDriverOverlappingBookings(any(), any(), any(), any()))
                .thenReturn(List.of(booking));

        assertThatThrownBy(() -> bookingService.createBooking("driver1", req))
                .isInstanceOf(BookingConflictException.class);
    }

    @Test
    void driverCancelsOwnBooking() {
        when(bookingRepository.findById(1000L)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(booking)).thenReturn(booking);

        BookingResponse response = bookingService.cancelBooking("driver1", 1000L, false);

        assertThat(response.status()).isEqualTo("CANCELLED");
    }

    @Test
    void cancelFailsWhenNotFound() {
        when(bookingRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.cancelBooking("driver1", 999L, false))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void driverCannotCancelOthers() {
        when(bookingRepository.findById(1000L)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.cancelBooking("otherdriver", 1000L, false))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void cannotCancelPastBooking() {
        booking.setBookingDate(YESTERDAY);
        when(bookingRepository.findById(1000L)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.cancelBooking("driver1", 1000L, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("past");
    }

    @Test
    void cannotCancelTwice() {
        booking.setStatus(Booking.Status.CANCELLED);
        when(bookingRepository.findById(1000L)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.cancelBooking("driver1", 1000L, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already cancelled");
    }

    @Test
    void reschedulesBooking() {
        RescheduleRequest req = new RescheduleRequest(TOMORROW.plusDays(1), LocalTime.of(14, 0), LocalTime.of(15, 0));
        Booking newBooking = new Booking(driver, connector, TOMORROW.plusDays(1), LocalTime.of(14, 0), LocalTime.of(15, 0));
        newBooking.setId(1001L);

        when(bookingRepository.findById(1000L)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenReturn(booking).thenReturn(newBooking);
        when(userService.getEntityByUsername("driver1")).thenReturn(driver);
        when(connectorService.getEntity(100L)).thenReturn(connector);
        when(bookingRepository.findConflictingBookingsForLock(any(), any(), any(), any()))
                .thenReturn(Collections.emptyList());
        when(bookingRepository.findDriverOverlappingBookings(any(), any(), any(), any()))
                .thenReturn(Collections.emptyList());

        BookingResponse response = bookingService.rescheduleBooking("driver1", 1000L, req);

        assertThat(response.id()).isEqualTo(1001L);
        assertThat(response.startTime()).isEqualTo(LocalTime.of(14, 0));
    }

    @Test
    void cannotRescheduleCancelled() {
        booking.setStatus(Booking.Status.CANCELLED);
        RescheduleRequest req = new RescheduleRequest(TOMORROW, LocalTime.of(14, 0), LocalTime.of(15, 0));
        when(bookingRepository.findById(1000L)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.rescheduleBooking("driver1", 1000L, req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cancelled");
    }

    @Test
    void driverCannotRescheduleOthers() {
        RescheduleRequest req = new RescheduleRequest(TOMORROW, LocalTime.of(14, 0), LocalTime.of(15, 0));
        when(bookingRepository.findById(1000L)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.rescheduleBooking("otherdriver", 1000L, req))
                .isInstanceOf(AccessDeniedException.class);
    }

}
