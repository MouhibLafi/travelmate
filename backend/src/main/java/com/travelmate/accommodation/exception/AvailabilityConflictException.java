package com.travelmate.accommodation.exception;

/** The requested dates overlap a booked period. Mapped to HTTP 409. */
public class AvailabilityConflictException extends RuntimeException {
    public AvailabilityConflictException(String message) {
        super(message);
    }
}
