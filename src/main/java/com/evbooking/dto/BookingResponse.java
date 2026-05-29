package com.evbooking.dto;

import com.evbooking.model.Booking;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record BookingResponse(
        Long id,
        Long userId,
        String username,
        Long connectorId,
        String connectorType,
        Long stationId,
        String stationName,
        LocalDate bookingDate,
        LocalTime startTime,
        LocalTime endTime,
        String status,
        LocalDateTime createdAt
) {
    public static BookingResponse from(Booking b) {
        return new BookingResponse(
                b.getId(),
                b.getUser().getId(),
                b.getUser().getUsername(),
                b.getConnector().getId(),
                b.getConnector().getConnectorType(),
                b.getConnector().getStation().getId(),
                b.getConnector().getStation().getName(),
                b.getBookingDate(),
                b.getStartTime(),
                b.getEndTime(),
                b.getStatus().name(),
                b.getCreatedAt()
        );
    }
}
