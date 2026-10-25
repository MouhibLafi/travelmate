package com.travelmate.itinerary.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travelmate.ai.OllamaClient;
import com.travelmate.exception.ApiException;
import com.travelmate.itinerary.catalog.CatalogItem;
import com.travelmate.itinerary.dto.TripInfo;
import com.travelmate.itinerary.entity.ItemType;
import com.travelmate.itinerary.service.AiItineraryService.AiDay;
import com.travelmate.itinerary.service.AiItineraryService.AiItem;
import com.travelmate.itinerary.service.AiItineraryService.AiPlan;
import com.travelmate.itinerary.service.ItineraryPlan.Slot;
import com.travelmate.users.entity.Interest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

// Tache IA 1 du module 6, SANS lancer Ollama : on donne volontairement de mauvaises reponses au service.
class AiItineraryServiceTest {

    // 3 jours, 2 nuits, 600 EUR -> hebergement 135 EUR/nuit max, activite 82,5 max, repas 41,25 max
    private static final TripInfo TRIP = new TripInfo(1L, "Rome, Italie",
            LocalDate.of(2026, 11, 10), LocalDate.of(2026, 11, 12), 600, false,
            List.of(Interest.HISTOIRE, Interest.GASTRONOMIE));

    private static final List<CatalogItem> HOTELS = List.of(
            hotel(301, "Auberge pas chère", 40, 3.5),
            hotel(302, "Bel hôtel", 120, 4.5),
            hotel(303, "Hôtel moyen", 90, 4.0),
            hotel(304, "Palace de luxe", 400, 4.9));

    private static final List<CatalogItem> ACTIVITIES = List.of(
            activity(101, "Visite du Colisée", "HISTOIRE", 55, 120, 4.8),
            activity(102, "Panthéon", "HISTOIRE", 5, 60, 4.6),
            activity(103, "Tour gastronomique", "GASTRONOMIE", 70, 180, 4.7),
            activity(104, "Tour en Vespa", "AVENTURE", 150, 180, 4.5),
            activity(105, "Promenade gratuite", "CULTURE", 0, 90, 4.4),
            activity(106, "Musée d'art", "CULTURE", 20, 120, 4.3),
            activity(107, "Catacombes", "HISTOIRE", 10, 90, 4.2));

    private static final List<CatalogItem> RESTAURANTS = List.of(
            restaurant(201, "Pizzeria", 12, 4.6),
            restaurant(202, "Trattoria", 30, 4.5),
            restaurant(203, "Marché couvert", 14, 4.4),
            restaurant(204, "Restaurant étoilé", 250, 4.9),
            restaurant(205, "Street food", 8, 4.3),
            restaurant(206, "Osteria", 35, 4.2),
            restaurant(207, "Café", 10, 4.1));

    private final ObjectMapper objectMapper = new ObjectMapper();
    private OllamaClient ollama;
    private AiItineraryService service;

    @BeforeEach
    void setUp() {
        ollama = mock(OllamaClient.class);
        service = new AiItineraryService(ollama);
    }

    @Test
    void envoieAuModeleSeulementCeQueLeBudgetPermet() throws Exception {
        answer(new AiPlan(302L, List.of()));

        service.plan(TRIP, ACTIVITIES, RESTAURANTS, HOTELS);

        ArgumentCaptor<String> prompt = ArgumentCaptor.forClass(String.class);
        verify(ollama).generateJson(prompt.capture());
        assertThat(prompt.getValue())
                .contains("hébergement au plus 135 EUR par nuit", "Bel hôtel", "Visite du Colisée", "Trattoria", "HISTOIRE")
                .doesNotContain("Palace de luxe", "Tour en Vespa", "Restaurant étoilé");
    }

    @Test
    void ignoreLesIdsInventesRepetesOuTropChers() throws Exception {
        answer(new AiPlan(302L, List.of(
                day(1, act(101, "09:00", "11:00"), act(999, "12:00", "13:00"), act(104, "14:00", "17:00"),
                        meal(204, "19:30")),
                day(2, act(101, "09:00", "11:00"), act(102, "11:30", "12:30")))));

        ItineraryPlan plan = service.plan(TRIP, ACTIVITIES, RESTAURANTS, HOTELS);

        assertThat(names(plan, 1)).contains("Visite du Colisée").doesNotContain("Tour en Vespa", "Restaurant étoilé");
        // Le Colisee est deja au jour 1 : le jour 2 ne garde que le Pantheon
        assertThat(names(plan, 2)).contains("Panthéon").doesNotContain("Visite du Colisée");
        assertThat(plan.aiUsed()).isTrue();
    }

    @Test
    void accepteLeTypeAvecAccentOuEnAnglais() throws Exception {
        answer(new AiPlan(302L, List.of(day(1,
                new AiItem("Activité", 101L, "09:00", "11:00", "note"),
                new AiItem("ACTIVITY", 102L, "14:00", "15:00", "note")))));

        ItineraryPlan plan = service.plan(TRIP, ACTIVITIES, RESTAURANTS, HOTELS);

        assertThat(activities(plan, 1)).extracting(s -> s.item().name()).containsExactly("Visite du Colisée", "Panthéon");
    }

    @Test
    void ignoreLesHeuresIllisiblesEtCalculeLaFinManquante() throws Exception {
        answer(new AiPlan(302L, List.of(day(1,
                act(101, "le matin", "11:00"),
                act(102, "9:30", "pas une heure")))));

        ItineraryPlan plan = service.plan(TRIP, ACTIVITIES, RESTAURANTS, HOTELS);

        List<Slot> activities = activities(plan, 1);
        assertThat(activities).extracting(s -> s.item().name()).containsExactly("Panthéon");
        // Fin calculee avec la duree du catalogue (60 min)
        assertThat(activities.get(0).start()).isEqualTo(LocalTime.of(9, 30));
        assertThat(activities.get(0).end()).isEqualTo(LocalTime.of(10, 30));
    }

    @Test
    void completeLesJoursOubliesAvecLesRegles() throws Exception {
        answer(new AiPlan(302L, List.of(day(1, act(101, "09:00", "11:00")))));

        ItineraryPlan plan = service.plan(TRIP, ACTIVITIES, RESTAURANTS, HOTELS);

        assertThat(plan.days()).extracting(ItineraryPlan.Day::dayNumber).containsExactly(1, 2, 3);
        assertThat(plan.days()).allSatisfy(d -> assertThat(d.slots()).isNotEmpty());
        assertThat(allActivityIds(plan)).doesNotHaveDuplicates();
    }

    @Test
    void ajouteLeDejeunerEtLeDinerOublies() throws Exception {
        answer(new AiPlan(302L, List.of(day(1, act(101, "09:00", "11:00")))));

        ItineraryPlan plan = service.plan(TRIP, ACTIVITIES, RESTAURANTS, HOTELS);

        assertThat(plan.days().get(0).slots())
                .filteredOn(s -> s.item().type() == ItemType.RESTAURANT)
                .extracting(Slot::start)
                .containsExactly(LocalTime.of(12, 30), LocalTime.of(19, 30));
    }

    @Test
    void garderLesRepasChoisisParLeModele() throws Exception {
        answer(new AiPlan(302L, List.of(day(1,
                act(101, "09:00", "11:00"), meal(202, "13:00"), meal(201, "20:00")))));

        ItineraryPlan plan = service.plan(TRIP, ACTIVITIES, RESTAURANTS, HOTELS);

        assertThat(plan.days().get(0).slots())
                .filteredOn(s -> s.item().type() == ItemType.RESTAURANT)
                .extracting(s -> s.item().name())
                .containsExactly("Trattoria", "Pizzeria");
    }

    @Test
    void decaleLesElementsQuiSeChevauchent() throws Exception {
        answer(new AiPlan(302L, List.of(day(1,
                act(101, "09:00", "11:00"),
                act(102, "10:00", "11:00"),
                meal(202, "12:30"), meal(201, "19:30")))));

        ItineraryPlan plan = service.plan(TRIP, ACTIVITIES, RESTAURANTS, HOTELS);

        List<Slot> slots = plan.days().get(0).slots();
        assertThat(slots.get(1).item().name()).isEqualTo("Panthéon");
        assertThat(slots.get(1).start()).isEqualTo(LocalTime.of(11, 0));
        assertThat(slots.get(1).end()).isEqualTo(LocalTime.of(12, 0));
        for (int i = 1; i < slots.size(); i++) {
            assertThat(slots.get(i).start()).isAfterOrEqualTo(slots.get(i - 1).end());
        }
    }

    @Test
    void choisitLeMeilleurHebergementAbordableSiLIdEstFaux() throws Exception {
        answer(new AiPlan(999L, List.of(day(1, act(101, "09:00", "11:00")))));

        ItineraryPlan plan = service.plan(TRIP, ACTIVITIES, RESTAURANTS, HOTELS);

        assertThat(plan.hotel().name()).isEqualTo("Bel hôtel");
    }

    @Test
    void utiliseLesReglesSiOllamaNeRepondPas() {
        when(ollama.generateJson(anyString()))
                .thenThrow(new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "L'IA ne répond pas."));

        ItineraryPlan plan = service.plan(TRIP, ACTIVITIES, RESTAURANTS, HOTELS);

        assertThat(plan.aiUsed()).isFalse();
        assertThat(plan.hotel()).isNotNull();
        assertThat(plan.days()).hasSize(3).allSatisfy(d -> assertThat(d.slots()).isNotEmpty());
        assertThat(allActivityIds(plan)).doesNotHaveDuplicates();
    }

    @Test
    void utiliseLesReglesSiLaReponseEstIllisible() {
        when(ollama.generateJson(anyString())).thenReturn("Voici votre itinéraire : jour 1...");

        ItineraryPlan plan = service.plan(TRIP, ACTIVITIES, RESTAURANTS, HOTELS);

        assertThat(plan.aiUsed()).isFalse();
        assertThat(plan.days()).hasSize(3);
    }

    @Test
    void refuseUnCatalogueVide() {
        assertThatThrownBy(() -> service.plan(TRIP, List.of(), List.of(), List.of()))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Rome, Italie");
        verifyNoInteractions(ollama);
    }

    // ---------- Outils ----------

    private void answer(AiPlan plan) throws Exception {
        when(ollama.generateJson(anyString())).thenReturn(objectMapper.writeValueAsString(plan));
    }

    private static AiDay day(int n, AiItem... items) {
        return new AiDay(n, "Jour " + n, List.of(items));
    }

    private static AiItem act(long id, String start, String end) {
        return new AiItem("ACTIVITE", id, start, end, "Parfait pour vous");
    }

    private static AiItem meal(long id, String start) {
        return new AiItem("RESTAURANT", id, start, null, "Bonne cuisine");
    }

    private static List<String> names(ItineraryPlan plan, int dayNumber) {
        return plan.days().get(dayNumber - 1).slots().stream().map(s -> s.item().name()).toList();
    }

    private static List<Slot> activities(ItineraryPlan plan, int dayNumber) {
        return plan.days().get(dayNumber - 1).slots().stream()
                .filter(s -> s.item().type() == ItemType.ACTIVITE).toList();
    }

    private static List<Long> allActivityIds(ItineraryPlan plan) {
        return plan.days().stream().flatMap(d -> d.slots().stream())
                .filter(s -> s.item().type() == ItemType.ACTIVITE).map(s -> s.item().id()).toList();
    }

    private static CatalogItem hotel(long id, String name, double price, double rating) {
        return new CatalogItem(id, ItemType.HEBERGEMENT, name, "hôtel", price, null, rating);
    }

    private static CatalogItem activity(long id, String name, String category, double price, int minutes, double rating) {
        return new CatalogItem(id, ItemType.ACTIVITE, name, category, price, minutes, rating);
    }

    private static CatalogItem restaurant(long id, String name, double price, double rating) {
        return new CatalogItem(id, ItemType.RESTAURANT, name, "restaurant", price, 60, rating);
    }
}
