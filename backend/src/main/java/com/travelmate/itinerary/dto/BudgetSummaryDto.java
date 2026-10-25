package com.travelmate.itinerary.dto;

import com.travelmate.itinerary.entity.ItemType;

import java.util.List;

// estimated = cout prevu de l'itineraire, spent = somme des depenses reelles
public record BudgetSummaryDto(double budget,
                               double estimatedTotal,
                               double spentTotal,
                               double remainingEstimated,
                               double remainingActual,
                               boolean overBudget,
                               List<CategoryBudget> categories) {

    public record CategoryBudget(ItemType category, double estimated, double spent) {
    }
}
