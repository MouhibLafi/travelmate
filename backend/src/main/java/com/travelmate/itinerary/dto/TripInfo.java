package com.travelmate.itinerary.dto;

import com.travelmate.trips.entity.Trip;
import com.travelmate.users.entity.BudgetLevel;
import com.travelmate.users.entity.Interest;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/*
 * Ce dont le module 6 a besoin d'un voyage du module 2.
 * Si le voyage n'a pas de montant, le budget est estime a partir de son niveau
 * (coherent avec les seuils du module 2 : ECONOMIQUE < 80 EUR/jour, MOYEN <= 200, LUXE au-dela).
 */
public record TripInfo(Long id,
                       String destination,
                       LocalDate startDate,
                       LocalDate endDate,
                       double budget,
                       boolean budgetEstimated,
                       List<Interest> interests) {

    static final double ECONOMIQUE_PER_DAY = 60;
    static final double MOYEN_PER_DAY = 140;
    static final double LUXE_PER_DAY = 300;

    public static TripInfo from(Trip trip, List<Interest> interests) {
        int days = (int) ChronoUnit.DAYS.between(trip.getStartDate(), trip.getEndDate()) + 1;
        boolean hasBudget = trip.getBudget() != null && trip.getBudget() > 0;
        double budget = hasBudget ? trip.getBudget() : perDay(trip.getBudgetLevel()) * days;
        return new TripInfo(trip.getId(), trip.getDestination(), trip.getStartDate(), trip.getEndDate(),
                budget, !hasBudget, interests);
    }

    static double perDay(BudgetLevel level) {
        if (level == BudgetLevel.ECONOMIQUE) return ECONOMIQUE_PER_DAY;
        if (level == BudgetLevel.LUXE) return LUXE_PER_DAY;
        return MOYEN_PER_DAY;   // MOYEN ou non renseigne
    }

    // Du 10 au 14 : 5 jours (le jour de depart compte)
    public int numberOfDays() {
        return (int) ChronoUnit.DAYS.between(startDate, endDate) + 1;
    }

    // Nuits d'hotel = jours - 1 (au moins 1)
    public int numberOfNights() {
        return Math.max(1, numberOfDays() - 1);
    }
}
