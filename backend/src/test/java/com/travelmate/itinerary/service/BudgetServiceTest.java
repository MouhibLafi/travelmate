package com.travelmate.itinerary.service;

import com.travelmate.itinerary.dto.BudgetSummaryDto;
import com.travelmate.itinerary.dto.BudgetSummaryDto.CategoryBudget;
import com.travelmate.itinerary.dto.TripInfo;
import com.travelmate.itinerary.entity.Expense;
import com.travelmate.itinerary.entity.ItemType;
import com.travelmate.itinerary.entity.Itinerary;
import com.travelmate.itinerary.entity.ItineraryDay;
import com.travelmate.itinerary.entity.ItineraryItem;
import com.travelmate.itinerary.repository.ExpenseRepository;
import com.travelmate.trips.entity.Trip;
import com.travelmate.users.entity.BudgetLevel;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

// Calculs de l'estimation du cout et du suivi du budget
class BudgetServiceTest {

    private static final String EMAIL = "yasmine@travelmate.tn";
    private static final LocalDate DAY_1 = LocalDate.of(2026, 11, 10);

    @Test
    void hebergementEgalPrixParNuitFoisNombreDeNuits() {
        assertThat(item(ItemType.HEBERGEMENT, 95, 4).getEstimatedCost()).isEqualTo(380);
    }

    @Test
    void quantiteAuMoinsUn() {
        ItineraryItem meal = item(ItemType.RESTAURANT, 20, 0);

        assertThat(meal.getQuantity()).isEqualTo(1);
        assertThat(meal.getEstimatedCost()).isEqualTo(20);
    }

    @Test
    void voyageDeCinqJoursAQuatreNuits() {
        Trip trip = trip(DAY_1, DAY_1.plusDays(4), 800.0, BudgetLevel.MOYEN);

        TripInfo info = TripInfo.from(trip, List.of());

        assertThat(info.numberOfDays()).isEqualTo(5);
        assertThat(info.numberOfNights()).isEqualTo(4);
        assertThat(info.budget()).isEqualTo(800);
        assertThat(info.budgetEstimated()).isFalse();
    }

    @Test
    void budgetEstimeSelonLeNiveauQuandLeVoyageNaPasDeMontant() {
        TripInfo luxe = TripInfo.from(trip(DAY_1, DAY_1.plusDays(2), null, BudgetLevel.LUXE), List.of());
        TripInfo economique = TripInfo.from(trip(DAY_1, DAY_1.plusDays(2), null, BudgetLevel.ECONOMIQUE), List.of());

        assertThat(luxe.budget()).isEqualTo(900);         // 3 jours x 300
        assertThat(economique.budget()).isEqualTo(180);   // 3 jours x 60
        assertThat(luxe.budgetEstimated()).isTrue();
    }

    @Test
    void resumeParCategoriePrevuEtDepense() {
        Itinerary it = new Itinerary();
        it.setBudget(300);
        ItineraryDay day = new ItineraryDay(1, DAY_1, "Jour 1");
        it.addDay(day);
        day.addItem(item(ItemType.HEBERGEMENT, 100, 2));
        day.addItem(item(ItemType.ACTIVITE, 30, 1));
        day.addItem(item(ItemType.RESTAURANT, 20, 1));
        it.recalculateTotal();
        it.getExpenses().add(expense(ItemType.HEBERGEMENT, 200));
        it.getExpenses().add(expense(ItemType.RESTAURANT, 25));

        BudgetSummaryDto summary = service(it).summary(EMAIL, 1L);

        assertThat(summary.estimatedTotal()).isEqualTo(250);
        assertThat(summary.spentTotal()).isEqualTo(225);
        assertThat(summary.remainingEstimated()).isEqualTo(50);
        assertThat(summary.remainingActual()).isEqualTo(75);
        assertThat(summary.overBudget()).isFalse();
        // Les categories sans cout (transport, autre) ne sont pas affichees
        assertThat(summary.categories()).containsExactly(
                new CategoryBudget(ItemType.ACTIVITE, 30, 0),
                new CategoryBudget(ItemType.RESTAURANT, 20, 25),
                new CategoryBudget(ItemType.HEBERGEMENT, 200, 200));
    }

    @Test
    void signaleUnDepassementDuBudget() {
        Itinerary it = new Itinerary();
        it.setBudget(100);
        ItineraryDay day = new ItineraryDay(1, DAY_1, "Jour 1");
        it.addDay(day);
        day.addItem(item(ItemType.HEBERGEMENT, 150, 1));
        it.recalculateTotal();

        BudgetSummaryDto summary = service(it).summary(EMAIL, 1L);

        assertThat(summary.overBudget()).isTrue();
        assertThat(summary.remainingEstimated()).isEqualTo(-50);
    }

    private static BudgetService service(Itinerary it) {
        ItineraryService itineraryService = mock(ItineraryService.class);
        when(itineraryService.findMyItinerary(EMAIL, 1L)).thenReturn(it);
        return new BudgetService(itineraryService, mock(ExpenseRepository.class));
    }

    private static Trip trip(LocalDate start, LocalDate end, Double budget, BudgetLevel level) {
        Trip trip = new Trip();
        trip.setId(1L);
        trip.setDestination("Rome");
        trip.setStartDate(start);
        trip.setEndDate(end);
        trip.setBudget(budget);
        trip.setBudgetLevel(level);
        return trip;
    }

    private static ItineraryItem item(ItemType type, double price, int quantity) {
        ItineraryItem item = new ItineraryItem();
        item.setType(type);
        item.setName(type.name());
        item.setPricing(price, quantity);
        return item;
    }

    private static Expense expense(ItemType category, double amount) {
        Expense e = new Expense();
        e.setCategory(category);
        e.setAmount(amount);
        e.setDate(DAY_1);
        return e;
    }
}
