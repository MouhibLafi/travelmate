package com.travelmate.accommodation.exception;

/** Ollama is down, timed out or answered something unusable. Mapped to HTTP 503 when not handled by a fallback. */
public class AiUnavailableException extends RuntimeException {
    public AiUnavailableException(String message) {
        super(message);
    }

    public AiUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
