package com.travelmate.itinerary.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travelmate.ai.OllamaClient;
import com.travelmate.exception.ApiException;
import com.travelmate.itinerary.catalog.CatalogItem;
import com.travelmate.itinerary.catalog.CatalogProvider;
import com.travelmate.itinerary.dto.OptimizationResultDto;
import com.travelmate.itinerary.dto.ReplacementDto;
import com.travelmate.itinerary.entity.ItemType;
import com.travelmate.itinerary.entity.Itinerary;
import com.travelmate.itinerary.entity.ItineraryDay;
import com.travelmate.itinerary.entity.ItineraryItem;
import com.travelmate.itinerary.service.BudgetOptimizerService.AiSwap;
import com.travelmate.itinerary.service.BudgetOptimizerService.AiSwaps;
import com.travelmate.trips.entity.Trip;
import com.travelmate.trips.repository.TripRepository;
import com.travelmate.users.entity.Interest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

// Tache IA 2 du module 6 (tenir le budget), avec un faux Ollama et un petit catalogue
class BudgetOptimizerServiceTest {

    private static final String EMAIL = "yasmine@travelmate.tn";
    private static final LocalDate DAY_1 = LocalDate.of(2026, 11, 10);

    // Petit catalogue, suffisant pour tester tous les chemins de l'optimiseur
    private static final CatalogProvider CATALOG = new CatalogProvider() {
        @Override
        public List<CatalogItem> getActivities(String destination) {
            return List.of(
                    new CatalogItem(101L, ItemType.ACTIVITE, "Visite guidée", "HISTOIRE", 60, 120, 4.8),
                    new CatalogItem(102L, ItemType.ACTIVITE, "Billet simple", "HISTOIRE", 20, 90, 4.5),
                    new CatalogItem(103L, ItemType.ACTIVITE, "Promenade gratuite", "CULTURE", 0, 90, 4.4));
        }

        @Override
        public List<CatalogItem> getRestaurants(String destination) {
            return List.of(
                    new CatalogItem(201L, ItemType.RESTAURANT, "Gastronomique", "restaurant", 80, 90, 4.9),
                    new CatalogItem(202L, ItemType.RESTAURANT, "Pizzeria", "restaurant", 12, 45, 4.6),
                    new CatalogItem(203L, ItemType.RESTAURANT, "Trattoria", "restaurant", 25, 60, 4.5));
        }

        @Override
        public List<CatalogItem> getHotels(String destination) {
            return List.of(
                    new CatalogItem(301L, ItemType.HEBERGEMENT, "Palace", "hôtel", 200, null, 4.9),
                    new CatalogItem(302L, ItemType.HEBERGEMENT, "Hôtel moyen", "hôtel", 100, null, 4.5),
                    new CatalogItem(303L, ItemType.HEBERGEMENT, "Auberge", "auberge", 40, null, 4.0));
        }
    };

    private final ObjectMapper objectMapper = new ObjectMapper();
    private OllamaClient ollama;
    private ItineraryService itineraryService;
    private BudgetOptimizerService optimizer;
    private long nextItemId = 1;

    @BeforeEach
    void setUp() {
        ollama = mock(OllamaClient.class);
        itineraryService = mock(ItineraryService.class);
        TripRepository trips = mock(TripRepository.class);
        Trip trip = new Trip();
        trip.setInterests(Set.of(Interest.HISTOIRE));
        when(trips.findById(anyLong())).thenReturn(Optional.of(trip));
        optimizer = new BudgetOptimizerService(itineraryService, CATALOG, trips, ollama);
    }

    @Test
    void neChangeRienSiLeBudgetEstTenu() {
        itinerary(1000, item(ItemType.HEBERGEMENT, 301, "Palace", 200, 2));

        OptimizationResultDto result = optimizer.optimize(EMAIL, 1L);

        assertThat(result.replacements()).isEmpty();
        assertThat(result.withinBudget()).isTrue();
        verifyNoInteractions(ollama);
    }

    @Test
    void sArreteDesQueLeBudgetEstTenu() throws Exception {
        // 400 + 60 + 80 = 540 pour un budget de 300
        Itinerary it = itinerary(300,
                item(ItemType.HEBERGEMENT, 301, "Palace", 200, 2),
                item(ItemType.ACTIVITE, 101, "Visite guidée", 60, 1),
                item(ItemType.RESTAURANT, 201, "Gastronomique", 80, 1));
        // Le modele propose 3 remplacements mais les 2 premiers suffisent : 540 - 200 - 40 = 300
        aiAnswers(new AiSwaps(List.of(
                new AiSwap(1L, 302L, "Confortable et moins cher"),
                new AiSwap(2L, 102L, "Même monument, sans guide"),
                new AiSwap(3L, 202L, "Dîner moins cher"))));

        OptimizationResultDto result = optimizer.optimize(EMAIL, 1L);

        assertThat(result.aiUsed()).isTrue();
        assertThat(result.withinBudget()).isTrue();
        assertThat(result.totalAfter()).isEqualTo(300);
        assertThat(result.replacements()).extracting(ReplacementDto::newName).containsExactly("Hôtel moyen", "Billet simple");
        assertThat(it.allItems()).extracting(ItineraryItem::getName).contains("Gastronomique");
    }

    @Test
    void ignoreLesRemplacementsInventesEtTermineAvecLeGlouton() throws Exception {
        itinerary(300,
                item(ItemType.HEBERGEMENT, 301, "Palace", 200, 2),
                item(ItemType.ACTIVITE, 101, "Visite guidée", 60, 1),
                item(ItemType.RESTAURANT, 201, "Gastronomique", 80, 1));
        aiAnswers(new AiSwaps(List.of(
                new AiSwap(999L, 302L, "Élément inconnu"),
                new AiSwap(1L, 301L, "Pas moins cher"),
                new AiSwap(2L, 777L, "Alternative inconnue"))));

        OptimizationResultDto result = optimizer.optimize(EMAIL, 1L);

        // 240 de trop : l'auberge (economise 320) est le seul remplacement qui suffit a lui seul
        assertThat(result.aiUsed()).isFalse();
        assertThat(result.withinBudget()).isTrue();
        assertThat(result.replacements()).extracting(ReplacementDto::newName).containsExactly("Auberge");
        assertThat(result.totalAfter()).isEqualTo(220);
    }

    @Test
    void utiliseLeGloutonSiOllamaNeRepondPas() {
        itinerary(300,
                item(ItemType.HEBERGEMENT, 301, "Palace", 200, 2),
                item(ItemType.RESTAURANT, 201, "Gastronomique", 80, 1));
        when(ollama.generateJson(anyString()))
                .thenThrow(new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "L'IA ne répond pas."));

        OptimizationResultDto result = optimizer.optimize(EMAIL, 1L);

        assertThat(result.aiUsed()).isFalse();
        assertThat(result.withinBudget()).isTrue();
        assertThat(result.totalAfter()).isLessThanOrEqualTo(300);
    }

    @Test
    void retireUneActivitePayanteEnDernierRecours() {
        // La promenade gratuite est deja prevue : le billet simple n'a plus d'alternative moins chere
        Itinerary it = itinerary(50,
                item(ItemType.HEBERGEMENT, 303, "Auberge", 40, 1),
                item(ItemType.ACTIVITE, 103, "Promenade gratuite", 0, 1),
                item(ItemType.ACTIVITE, 102, "Billet simple", 20, 1));

        OptimizationResultDto result = optimizer.optimize(EMAIL, 1L);

        assertThat(result.withinBudget()).isTrue();
        assertThat(result.replacements()).singleElement()
                .satisfies(r -> assertThat(r.newName()).isEqualTo("(retirée, temps libre)"));
        assertThat(it.allItems()).extracting(ItineraryItem::getName).doesNotContain("Billet simple");
    }

    @Test
    void previentQuandLeBudgetEstImpossible() {
        itinerary(10, item(ItemType.HEBERGEMENT, 303, "Auberge", 40, 1));

        OptimizationResultDto result = optimizer.optimize(EMAIL, 1L);

        assertThat(result.withinBudget()).isFalse();
        assertThat(result.message()).contains("encore 30 € au-dessus du budget");
    }

    // ---------- Outils ----------

    private void aiAnswers(AiSwaps swaps) throws Exception {
        when(ollama.generateJson(anyString())).thenReturn(objectMapper.writeValueAsString(swaps));
    }

    // Itineraire d'une journee, renvoye par le faux ItineraryService
    private Itinerary itinerary(double budget, ItineraryItem... items) {
        Itinerary it = new Itinerary();
        it.setId(1L);
        it.setTripId(1L);
        it.setDestination("Rome");
        it.setStartDate(DAY_1);
        it.setEndDate(DAY_1);
        it.setBudget(budget);
        ItineraryDay day = new ItineraryDay(1, DAY_1, "Jour 1");
        it.addDay(day);
        for (ItineraryItem item : items) {
            day.addItem(item);
        }
        it.recalculateTotal();
        when(itineraryService.findMyItinerary(EMAIL, 1L)).thenReturn(it);
        return it;
    }

    private ItineraryItem item(ItemType type, long refId, String name, double price, int quantity) {
        ItineraryItem item = new ItineraryItem();
        item.setId(nextItemId++);
        item.setType(type);
        item.setRefId(refId);
        item.setName(name);
        item.setCategory(switch (type) {
            case HEBERGEMENT -> refId == 303 ? "auberge" : "hôtel";
            case RESTAURANT -> "restaurant";
            default -> refId == 103 ? "CULTURE" : "HISTOIRE";
        });
        item.setStartTime(LocalTime.of(8, 0).plusHours(nextItemId));
        item.setPricing(price, quantity);
        return item;
    }
}
