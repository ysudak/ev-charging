package com.evbooking.repository;

import com.evbooking.model.Booking;
import com.evbooking.model.ChargingStation;
import com.evbooking.model.Connector;
import com.evbooking.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class BookingRepositoryTest {

    @Autowired private TestEntityManager em;
    @Autowired private BookingRepository bookingRepository;

    private User driver;
    private ChargingStation station;
    private Connector connector;

    @BeforeEach
    void setUp() {
        driver = em.persist(new User("driver1", "encoded", User.Role.DRIVER));
        station = em.persist(new ChargingStation("Station A", "123 Main St", 40.0, -74.0, "desc"));
        connector = em.persist(new Connector(station, "CCS2", 50));
        em.flush();
    }

    @Test
    void returnsOnlyConfirmedForDate() {
        LocalDate date = LocalDate.now().plusDays(1);
        em.persist(new Booking(driver, connector, date, LocalTime.of(10, 0), LocalTime.of(11, 0)));
        Booking cancelled = new Booking(driver, connector, date, LocalTime.of(12, 0), LocalTime.of(13, 0));
        cancelled.setStatus(Booking.Status.CANCELLED);
        em.persist(cancelled);
        em.flush();

        List<Booking> result = bookingRepository.findConfirmedByConnectorAndDate(connector.getId(), date);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStartTime()).isEqualTo(LocalTime.of(10, 0));
    }

    @Test
    void detectsOverlappingBooking() {
        LocalDate date = LocalDate.now().plusDays(1);
        em.persist(new Booking(driver, connector, date, LocalTime.of(10, 0), LocalTime.of(12, 0)));
        em.flush();

        List<Booking> conflicts = bookingRepository.findConflictingBookingsForLock(
                connector.getId(), date, LocalTime.of(11, 0), LocalTime.of(13, 0));

        assertThat(conflicts).hasSize(1);
    }

    @Test
    void adjacentTimesDoNotConflict() {
        LocalDate date = LocalDate.now().plusDays(1);
        em.persist(new Booking(driver, connector, date, LocalTime.of(10, 0), LocalTime.of(11, 0)));
        em.flush();

        List<Booking> conflicts = bookingRepository.findConflictingBookingsForLock(
                connector.getId(), date, LocalTime.of(11, 0), LocalTime.of(12, 0));

        assertThat(conflicts).isEmpty();
    }

    @Test
    void cancelledBookingsDoNotConflict() {
        LocalDate date = LocalDate.now().plusDays(1);
        Booking cancelled = new Booking(driver, connector, date, LocalTime.of(10, 0), LocalTime.of(12, 0));
        cancelled.setStatus(Booking.Status.CANCELLED);
        em.persist(cancelled);
        em.flush();

        List<Booking> conflicts = bookingRepository.findConflictingBookingsForLock(
                connector.getId(), date, LocalTime.of(10, 0), LocalTime.of(12, 0));

        assertThat(conflicts).isEmpty();
    }

    @Test
    void detectsDriverOverlap() {
        LocalDate date = LocalDate.now().plusDays(1);
        em.persist(new Booking(driver, connector, date, LocalTime.of(14, 0), LocalTime.of(15, 0)));
        em.flush();

        List<Booking> result = bookingRepository.findDriverOverlappingBookings(
                driver.getId(), date, LocalTime.of(14, 30), LocalTime.of(15, 30));

        assertThat(result).hasSize(1);
    }

    @Test
    void returnsPastConfirmedOnly() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        em.persist(new Booking(driver, connector, yesterday, LocalTime.of(10, 0), LocalTime.of(11, 0)));
        em.persist(new Booking(driver, connector, tomorrow, LocalTime.of(10, 0), LocalTime.of(11, 0)));
        em.flush();

        List<Booking> result = bookingRepository.findPastConfirmedBookings(LocalDate.now());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getBookingDate()).isEqualTo(yesterday);
    }

    @Test
    void excludesCancelledFromPastQuery() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        Booking cancelled = new Booking(driver, connector, yesterday, LocalTime.of(10, 0), LocalTime.of(11, 0));
        cancelled.setStatus(Booking.Status.CANCELLED);
        em.persist(cancelled);
        em.flush();

        List<Booking> result = bookingRepository.findPastConfirmedBookings(LocalDate.now());

        assertThat(result).isEmpty();
    }

    @Test
    void countsBookingsByStationForToday() {
        LocalDate today = LocalDate.now();
        User driver2 = em.persist(new User("driver2", "encoded", User.Role.DRIVER));
        em.persist(new Booking(driver, connector, today, LocalTime.of(9, 0), LocalTime.of(10, 0)));
        em.persist(new Booking(driver2, connector, today, LocalTime.of(11, 0), LocalTime.of(12, 0)));
        em.flush();

        List<Object[]> result = bookingRepository.countTodayGroupedByStation(today);

        assertThat(result).hasSize(1);
        assertThat((Long) result.get(0)[0]).isEqualTo(station.getId());
        assertThat((Long) result.get(0)[1]).isEqualTo(2L);
    }

}
