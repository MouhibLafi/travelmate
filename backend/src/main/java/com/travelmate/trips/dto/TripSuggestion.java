package com.travelmate.trips.dto;

import com.travelmate.users.entity.BudgetLevel;
import com.travelmate.users.entity.Interest;

import java.time.LocalDate;
import java.util.List;

// Resultat de l'IA du module 2 : le formulaire pre-rempli.
// Ce n'est qu'une SUGGESTION : l'utilisateur la verifie avant de creer le voyage.
public record TripSuggestion(
        String destination,     // null si aucune destination n'est citee
        LocalDate startDate,
        LocalDate endDate,
        Double budget,          // en euros, null si non indique
        List<Interest> interests,
        BudgetLevel budgetLevel,
        double confidence,      // entre 0 et 1
        String explanation
) {
}
