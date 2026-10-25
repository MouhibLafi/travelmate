package com.travelmate.itinerary.dto;

import jakarta.validation.constraints.NotNull;

public record GenerateRequest(@NotNull(message = "Le voyage est obligatoire") Long tripId) {
}
