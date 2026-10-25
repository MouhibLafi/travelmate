package com.travelmate.itinerary.dto;

import com.travelmate.itinerary.entity.ItemType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.LocalTime;

// Ajouter ou modifier un element. Avec un refId du catalogue, le nom et le prix vides sont repris du catalogue.
public record ItemRequest(@NotNull(message = "Le type est obligatoire") ItemType type,
                          Long refId,
                          String name,
                          String category,
                          LocalTime startTime,
                          LocalTime endTime,
                          @PositiveOrZero(message = "Le prix ne peut pas être négatif") Double unitPrice,
                          @Min(value = 1, message = "La quantité doit être au moins 1") Integer quantity,
                          String notes) {
}
