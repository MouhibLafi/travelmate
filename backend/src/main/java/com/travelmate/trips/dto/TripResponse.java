package com.travelmate.trips.dto;

import com.travelmate.trips.entity.Trip;
import com.travelmate.users.entity.BudgetLevel;
import com.travelmate.users.entity.Interest;

import java.time.LocalDate;
import java.util.Set;

// Ce que l'API renvoie (on n'expose pas l'entite User complete)
public record TripResponse(
        Long id,
        String destination,
        LocalDate startDate,
        LocalDate endDate,
        Double budget,
        Set<Interest> interests,
        BudgetLevel budgetLevel
) {
    public static TripResponse from(Trip trip) {
        return new TripResponse(trip.getId(), trip.getDestination(), trip.getStartDate(), trip.getEndDate(),
                trip.getBudget(), trip.getInterests(), trip.getBudgetLevel());
    }
}
