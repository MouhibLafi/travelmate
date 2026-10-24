package com.travelmate.trips.dto;

import com.travelmate.users.entity.BudgetLevel;
import com.travelmate.users.entity.Interest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;
import java.util.Set;

// Donnees envoyees par le formulaire "Creer / modifier un voyage"
public record TripRequest(
        @NotBlank(message = "La destination est obligatoire") String destination,
        @NotNull(message = "La date de début est obligatoire") LocalDate startDate,
        @NotNull(message = "La date de fin est obligatoire") LocalDate endDate,
        @Positive(message = "Le budget doit être positif") Double budget,
        Set<Interest> interests,
        BudgetLevel budgetLevel
) {
}
