package com.evbooking.service;

import com.evbooking.dto.BookedTimeResponse;
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
    void getBookedTimes_returnsTimesForConnectorAndDate() {
        when(bookingRepository.findConfirmedByConnectorAndDate(100L, TOMORROW))
                .thenReturn(List.of(booking));

        List<BookedTimeResponse> result = bookingService.getBookedTimes(100L, TOMORROW);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).startTime()).isEqualTo(LocalTime.of(10, 0));
        assertThat(result.get(0).endTime()).isEqualTo(LocalTime.of(11, 0));
    }

    @Test
    void createBooking_success() {
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
    void createBooking_endTimeNotAfterStartTime_throws() {
        BookingRequest req = new BookingRequest(100L, TOMORROW, LocalTime.of(11, 0), LocalTime.of(10, 0));

        assertThatThrownBy(() -> bookingService.createBooking("driver1", req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("End time must be after start time");
    }

    @Test
    void createBooking_pastDate_throws() {
        BookingRequest req = new BookingRequest(100L, YESTERDAY, LocalTime.of(10, 0), LocalTime.of(11, 0));

        assertThatThrownBy(() -> bookingService.createBooking("driver1", req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("past");
    }

    @Test
    void createBooking_adminRole_throws() {
        BookingRequest req = new BookingRequest(100L, TOMORROW, LocalTime.of(10, 0), LocalTime.of(11, 0));
        when(userService.getEntityByUsername("admin1")).thenReturn(admin);

        assertThatThrownBy(() -> bookingService.createBooking("admin1", req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Administrators cannot make reservations");
    }

    @Test
    void createBooking_connectorConflict_throws() {
        BookingRequest req = new BookingRequest(100L, TOMORROW, LocalTime.of(10, 0), LocalTime.of(11, 0));
        when(userService.getEntityByUsername("driver1")).thenReturn(driver);
        when(connectorService.getEntity(100L)).thenReturn(connector);
        when(bookingRepository.findConflictingBookingsForLock(any(), any(), any(), any()))
                .thenReturn(List.of(booking));

        assertThatThrownBy(() -> bookingService.createBooking("driver1", req))
                .isInstanceOf(BookingConflictException.class);
    }

    @Test
    void createBooking_driverConflict_throws() {
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
    void cancelBooking_ownBooking_success() {
        when(bookingRepository.findById(1000L)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(booking)).thenReturn(booking);

        BookingResponse response = bookingService.cancelBooking("driver1", 1000L, false);

        assertThat(response.status()).isEqualTo("CANCELLED");
    }

    @Test
    void cancelBooking_adminCancelsOthersBooking_success() {
        when(bookingRepository.findById(1000L)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(booking)).thenReturn(booking);

        BookingResponse response = bookingService.cancelBooking("admin1", 1000L, true);

        assertThat(response.status()).isEqualTo("CANCELLED");
    }

    @Test
    void cancelBooking_notFound_throws() {
        when(bookingRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.cancelBooking("driver1", 999L, false))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void cancelBooking_notOwnerNotAdmin_throws() {
        when(bookingRepository.findById(1000L)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.cancelBooking("otherdriver", 1000L, false))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void cancelBooking_pastBooking_throws() {
        booking.setBookingDate(YESTERDAY);
        when(bookingRepository.findById(1000L)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.cancelBooking("driver1", 1000L, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("past");
    }

    @Test
    void cancelBooking_alreadyCancelled_throws() {
        booking.setStatus(Booking.Status.CANCELLED);
        when(bookingRepository.findById(1000L)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.cancelBooking("driver1", 1000L, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already cancelled");
    }

    @Test
    void rescheduleBooking_success() {
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
    void rescheduleBooking_cancelledBooking_throws() {
        booking.setStatus(Booking.Status.CANCELLED);
        RescheduleRequest req = new RescheduleRequest(TOMORROW, LocalTime.of(14, 0), LocalTime.of(15, 0));
        when(bookingRepository.findById(1000L)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.rescheduleBooking("driver1", 1000L, req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cancelled");
    }

    @Test
    void rescheduleBooking_notOwner_throws() {
        RescheduleRequest req = new RescheduleRequest(TOMORROW, LocalTime.of(14, 0), LocalTime.of(15, 0));
        when(bookingRepository.findById(1000L)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.rescheduleBooking("otherdriver", 1000L, req))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void findForUser_returnsUserBookings() {
        when(userService.getEntityByUsername("driver1")).thenReturn(driver);
        when(bookingRepository.findByUserIdOrderByBookingDateDescStartTimeDesc(1L))
                .thenReturn(List.of(booking));

        List<BookingResponse> result = bookingService.findForUser("driver1");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).username()).isEqualTo("driver1");
    }

    @Test
    void findAll_returnsAllBookings() {
        when(bookingRepository.findAllByOrderByBookingDateDescStartTimeDesc())
                .thenReturn(List.of(booking));

        List<BookingResponse> result = bookingService.findAll();

        assertThat(result).hasSize(1);
    }

    @Test
    void findById_found() {
        when(bookingRepository.findById(1000L)).thenReturn(Optional.of(booking));

        BookingResponse result = bookingService.findById(1000L);

        assertThat(result.id()).isEqualTo(1000L);
    }

    @Test
    void findById_notFound_throws() {
        when(bookingRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.findById(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
