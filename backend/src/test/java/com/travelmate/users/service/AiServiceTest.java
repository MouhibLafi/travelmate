package com.travelmate.users.service;

import com.travelmate.users.dto.AiResponse;
import com.travelmate.users.entity.BudgetLevel;
import com.travelmate.users.entity.Interest;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

// Teste la lecture de la reponse du modele, SANS lancer Ollama.
class AiServiceTest {

    // null : on ne teste que parseAnswer, sans appeler Ollama
    private final AiService aiService = new AiService(null);

    @Test
    void reponseCorrecte() {
        String json = """
                {"interests": ["PLAGE", "GASTRONOMIE"], "budgetLevel": "ECONOMIQUE",
                 "confidence": 0.9, "explanation": "Plage et cuisine, petit budget."}
                """;

        AiResponse result = aiService.parseAnswer(json);

        assertEquals(List.of(Interest.PLAGE, Interest.GASTRONOMIE), result.interests());
        assertEquals(BudgetLevel.ECONOMIQUE, result.budgetLevel());
        assertEquals(0.9, result.confidence());
    }

    @Test
    void valeursInventeesIgnorees() {
        String json = """
                {"interests": ["CASINO", "vie nocturne", "PLAGE"], "budgetLevel": "PAS_CHER",
                 "confidence": 0.8, "explanation": ""}
                """;

        AiResponse result = aiService.parseAnswer(json);

        // "CASINO" n'existe pas -> ignore ; "vie nocturne" -> VIE_NOCTURNE
        assertEquals(List.of(Interest.VIE_NOCTURNE, Interest.PLAGE), result.interests());
        // budget invalide -> MOYEN avec une confiance faible
        assertEquals(BudgetLevel.MOYEN, result.budgetLevel());
        assertEquals(0.3, result.confidence());
    }
}
