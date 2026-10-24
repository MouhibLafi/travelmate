package com.travelmate.users.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AiRequest(
        @NotBlank(message = "Décrivez-vous en une phrase")
        @Size(min = 10, max = 500, message = "La phrase doit contenir entre 10 et 500 caractères")
        String description
) {
}
