package com.travelmate.trips.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// La demande du voyageur en texte libre, ex. : "5 jours à Rome, 800 €"
public record TripAiRequest(
        @NotBlank(message = "Décrivez votre voyage")
        @Size(min = 5, max = 500, message = "La demande doit contenir entre 5 et 500 caractères")
        String text
) {
}
