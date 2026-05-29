package com.evbooking.repository;

import com.evbooking.model.Booking;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findByUserIdOrderByBookingDateDescStartTimeDesc(Long userId);

    List<Booking> findAllByOrderByBookingDateDescStartTimeDesc();

    /**
     * pessimistic write lock so two concurrent requests dont both see zero conflicts
     * and create overlapping bookings at the same time. any transaction trying to
     * read these rows will block until the first one commits or rolls back.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Booking b WHERE b.connector.id = :connectorId " +
           "AND b.bookingDate = :date " +
           "AND b.status = 'CONFIRMED' " +
           "AND b.startTime < :endTime " +
           "AND b.endTime > :startTime")
    List<Booking> findConflictingBookingsForLock(
            @Param("connectorId") Long connectorId,
            @Param("date") LocalDate date,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime);

    /** all confirmed bookings for a connector on given date, used to show occupied times in the ui */
    @Query("SELECT b FROM Booking b WHERE b.connector.id = :connectorId " +
           "AND b.bookingDate = :date " +
           "AND b.status = 'CONFIRMED' " +
           "ORDER BY b.startTime")
    List<Booking> findConfirmedByConnectorAndDate(
            @Param("connectorId") Long connectorId,
            @Param("date") LocalDate date);

    /** checks if the driver already has an overlapping booking somewhere else */
    @Query("SELECT b FROM Booking b WHERE b.user.id = :userId " +
           "AND b.bookingDate = :date " +
           "AND b.status = 'CONFIRMED' " +
           "AND b.startTime < :endTime " +
           "AND b.endTime > :startTime")
    List<Booking> findDriverOverlappingBookings(
            @Param("userId") Long userId,
            @Param("date") LocalDate date,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime);

    /** finds CONFIRMED bookings whose date is before today, ready to be marked COMPLETED */
    @Query("SELECT b FROM Booking b WHERE b.status = 'CONFIRMED' AND b.bookingDate < :today")
    List<Booking> findPastConfirmedBookings(@Param("today") LocalDate today);

    /** returns [stationId, count] pairs for all confirmed bookings on a given date, one row per station */
    @Query("SELECT b.connector.station.id, COUNT(b) FROM Booking b " +
           "WHERE b.bookingDate = :date AND b.status = 'CONFIRMED' " +
           "GROUP BY b.connector.station.id")
    List<Object[]> countTodayGroupedByStation(@Param("date") LocalDate date);
}
