package com.travelmate.trips.service;

import com.travelmate.trips.dto.TripSuggestion;
import com.travelmate.users.entity.BudgetLevel;
import com.travelmate.users.entity.Interest;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Teste la verification et les calculs de la tache IA du module 2, SANS lancer Ollama.
class TripAiServiceTest {

    // null : on ne teste que parseAnswer, sans appeler Ollama
    private final TripAiService tripAiService = new TripAiService(null);
    private final LocalDate today = LocalDate.of(2026, 10, 24);

    @Test
    void exempleDuSujet() {
        // "5 jours a Rome, 800 €"
        String json = """
                {"destination": "Rome", "startDate": null, "durationDays": 5, "budget": 800,
                 "interests": [], "confidence": 0.9, "explanation": "Rome, 5 jours, 800 €."}
                """;

        TripSuggestion result = tripAiService.parseAnswer(json, today);

        assertEquals("Rome", result.destination());
        assertEquals(today.plusDays(7), result.startDate());          // pas de date -> dans 7 jours
        assertEquals(today.plusDays(7 + 4), result.endDate());         // 5 jours : debut + 4
        assertEquals(800.0, result.budget());
        assertEquals(BudgetLevel.MOYEN, result.budgetLevel());         // 800 / 5 = 160 € par jour
        assertEquals(0.9, result.confidence());
    }

    @Test
    void dateIndiqueeEtInterets() {
        String json = """
                {"destination": "Tunis", "startDate": "2026-12-20", "durationDays": 3, "budget": 150,
                 "interests": ["HISTOIRE", "gastronomie"], "confidence": 0.8, "explanation": ""}
                """;

        TripSuggestion result = tripAiService.parseAnswer(json, today);

        assertEquals(LocalDate.of(2026, 12, 20), result.startDate());
        assertEquals(LocalDate.of(2026, 12, 22), result.endDate());
        assertEquals(List.of(Interest.HISTOIRE, Interest.GASTRONOMIE), result.interests());
        assertEquals(BudgetLevel.ECONOMIQUE, result.budgetLevel());   // 150 / 3 = 50 € par jour
    }

    @Test
    void valeursInventeesOuInvalidesCorrigees() {
        String json = """
                {"destination": "", "startDate": "2020-01-01", "durationDays": 400, "budget": -5,
                 "interests": ["CASINO", "PLAGE"], "confidence": 3, "explanation": ""}
                """;

        TripSuggestion result = tripAiService.parseAnswer(json, today);

        assertNull(result.destination());                              // vide -> null
        assertEquals(today.plusDays(7), result.startDate());          // date passee -> dans 7 jours
        assertEquals(result.startDate().plusDays(6), result.endDate()); // duree invalide -> 7 jours
        assertNull(result.budget());                                   // budget negatif -> null
        assertNull(result.budgetLevel());
        assertEquals(List.of(Interest.PLAGE), result.interests());     // CASINO n'existe pas
        assertTrue(result.confidence() <= 0.5);                        // confiance baissee
    }

    @Test
    void sansMontantNiveauDeduitDesMots() {
        // "Des vacances au soleil pas cheres" : pas de montant, mais "pas cheres"
        String json = """
                {"destination": null, "durationDays": null, "budget": null, "budgetLevel": "ECONOMIQUE",
                 "interests": ["PLAGE"], "confidence": 0.6, "explanation": ""}
                """;

        TripSuggestion result = tripAiService.parseAnswer(json, today);

        assertNull(result.budget());
        assertEquals(BudgetLevel.ECONOMIQUE, result.budgetLevel());
    }

    @Test
    void destinationEcriteProprement() {
        assertEquals("Rome", TripAiService.capitalize("ROME"));
        assertEquals("New York", TripAiService.capitalize("new york"));
        assertEquals("Aix-En-Provence", TripAiService.capitalize("AIX-EN-PROVENCE"));
        assertNull(TripAiService.capitalize(null));
    }

    @Test
    void niveauDeBudgetParJour() {
        assertEquals(BudgetLevel.ECONOMIQUE, TripAiService.budgetLevel(300.0, 7));  // 43 €/jour
        assertEquals(BudgetLevel.MOYEN, TripAiService.budgetLevel(1400.0, 7));      // 200 €/jour
        assertEquals(BudgetLevel.LUXE, TripAiService.budgetLevel(3000.0, 5));       // 600 €/jour
        assertNull(TripAiService.budgetLevel(null, 5));
    }
}
