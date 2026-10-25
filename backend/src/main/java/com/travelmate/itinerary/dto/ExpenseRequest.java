package com.travelmate.itinerary.dto;

import com.travelmate.itinerary.entity.ItemType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;

public record ExpenseRequest(@NotNull(message = "La catégorie est obligatoire") ItemType category,
                             @NotNull(message = "Le montant est obligatoire")
                             @Positive(message = "Le montant doit être positif") Double amount,
                             LocalDate date,
                             String description) {
}
