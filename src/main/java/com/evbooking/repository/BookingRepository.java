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

    @Query("SELECT b FROM Booking b WHERE b.connector.id = :connectorId " +
           "AND b.bookingDate = :date " +
           "AND b.status = 'CONFIRMED' " +
           "ORDER BY b.startTime")
    List<Booking> findConfirmedByConnectorAndDate(
            @Param("connectorId") Long connectorId,
            @Param("date") LocalDate date);

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

    @Query("SELECT b FROM Booking b WHERE b.status = 'CONFIRMED' AND b.bookingDate < :today")
    List<Booking> findPastConfirmedBookings(@Param("today") LocalDate today);

    @Query("SELECT b.connector.station.id, COUNT(b) FROM Booking b " +
           "WHERE b.bookingDate = :date AND b.status = 'CONFIRMED' " +
           "GROUP BY b.connector.station.id")
    List<Object[]> countTodayGroupedByStation(@Param("date") LocalDate date);
}
