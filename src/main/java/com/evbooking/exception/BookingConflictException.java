package com.evbooking.exception;

/** Thrown when a new booking overlaps an existing confirmed booking on the same connector. */
public class BookingConflictException extends RuntimeException {

    public BookingConflictException(String message) {
        super(message);
    }
}
