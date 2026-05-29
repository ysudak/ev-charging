package com.evbooking.dto;

import com.evbooking.model.Booking;

import java.time.LocalTime;

public record BookedTimeResponse(LocalTime startTime, LocalTime endTime) {

    public static BookedTimeResponse from(Booking b) {
        return new BookedTimeResponse(b.getStartTime(), b.getEndTime());
    }
}
