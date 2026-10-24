package com.travelmate.users.dto;

import com.travelmate.users.entity.BudgetLevel;
import com.travelmate.users.entity.Interest;

import java.util.List;

// Resultat de l'IA. Ce n'est qu'une SUGGESTION : l'utilisateur la valide avant d'enregistrer.
public record AiResponse(
        List<Interest> interests,
        BudgetLevel budgetLevel,
        double confidence,      // entre 0 et 1, estimee par le modele
        String explanation
) {
}
