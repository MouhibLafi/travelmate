package com.travelmate.accommodation.exception;

/** Business validation error (bad dates, too many guests, unsupported sort...) mapped to HTTP 400. */
public class InvalidRequestException extends RuntimeException {
    public InvalidRequestException(String message) {
        super(message);
    }
}
