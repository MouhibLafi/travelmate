package com.travelmate.exception;

import org.springframework.http.HttpStatus;

// Une seule exception pour toute l'application : un code HTTP + un message.
// Exemple : throw new ApiException(HttpStatus.NOT_FOUND, "Utilisateur introuvable");
public class ApiException extends RuntimeException {

    private final HttpStatus status;

    public ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
