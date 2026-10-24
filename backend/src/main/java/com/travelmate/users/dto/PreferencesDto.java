package com.travelmate.users.dto;

import com.travelmate.users.entity.BudgetLevel;
import com.travelmate.users.entity.Interest;
import com.travelmate.users.entity.TravelPreferences;

import java.util.Set;

// Utilise pour lire (GET) ET pour modifier (PUT) les preferences.
public record PreferencesDto(
        String description,
        Set<Interest> interests,
        BudgetLevel budgetLevel,
        boolean aiGenerated
) {
    public static PreferencesDto from(TravelPreferences p) {
        return new PreferencesDto(p.getDescription(), p.getInterests(), p.getBudgetLevel(), p.isAiGenerated());
    }
}
