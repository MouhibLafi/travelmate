package com.travelmate.itinerary.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.travelmate.ai.OllamaClient;
import com.travelmate.exception.ApiException;
import com.travelmate.itinerary.catalog.CatalogItem;
import com.travelmate.itinerary.dto.TripInfo;
import com.travelmate.itinerary.entity.ItemType;
import com.travelmate.itinerary.service.ItineraryPlan.Day;
import com.travelmate.itinerary.service.ItineraryPlan.Slot;
import com.travelmate.users.entity.Interest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/*
 * TACHE IA DU MODULE 6 (1/2) : generer un itineraire personnalise jour par jour.
 * Meme principe que le module 2 : l'IA CHOISIT, Java VERIFIE et CALCULE.
 *   1. Java garde seulement ce que le budget permet (un modele de 8 milliards de parametres additionne mal les prix)
 *   2. le prompt donne le voyage, une limite par nuit et par jour, et le catalogue avec ses ids
 *   3. appel a llama3 via le client commun OllamaClient
 *   4. verification : ids inconnus ou repetes ignores, heures illisibles ignorees, dejeuner ou diner manquant ajoute,
 *      chevauchements decales, jours oublies completes par les regles ; les prix viennent toujours du catalogue
 * Si Ollama ne repond pas, un plan est construit par des regles simples (aiUsed = false) : le module marche toujours.
 */
@Service
public class AiItineraryService {

    private static final Logger log = LoggerFactory.getLogger(AiItineraryService.class);
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("H:mm[:ss]");

    private final OllamaClient ollamaClient;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    public AiItineraryService(OllamaClient ollamaClient) {
        this.ollamaClient = ollamaClient;
    }

    public ItineraryPlan plan(TripInfo trip, List<CatalogItem> activities,
                              List<CatalogItem> restaurants, List<CatalogItem> hotels) {
        if (hotels.isEmpty() && activities.isEmpty() && restaurants.isEmpty()) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Pas encore d'activités, de restaurants ni d'hébergements pour "
                    + trip.destination() + ". Destinations disponibles : Rome, Paris, Tunis, Djerba, Marrakech.");
        }
        try {
            // On ne montre au modele que ce que le budget permet
            Limits limits = limits(trip);
            List<CatalogItem> aiHotels = affordable(hotels, limits.hotelPerNight(), 3);
            List<CatalogItem> aiActivities = affordable(activities, limits.activity(), 2 * trip.numberOfDays());
            List<CatalogItem> aiRestaurants = affordable(restaurants, limits.meal(), 6);
            String answer = ollamaClient.generateJson(buildPrompt(trip, limits, aiActivities, aiRestaurants, aiHotels));
            AiPlan aiPlan = objectMapper.readValue(answer, AiPlan.class);
            return validate(aiPlan, trip, aiActivities, aiRestaurants, aiHotels);
        } catch (Exception e) {
            log.warn("Itineraire IA impossible, plan construit par les regles : {}", e.getMessage());
            return fallbackPlan(trip, activities, restaurants, hotels);
        }
    }

    // ---------- 1. Budget et prompt ----------

    /*
     * Repartition du budget pour filtrer le catalogue : 45 % maximum pour l'hebergement (comme pickHotel),
     * le reste partage entre les jours, moitie repas, moitie activites.
     */
    private Limits limits(TripInfo trip) {
        double hotelPerNight = trip.budget() * 0.45 / trip.numberOfNights();
        double perDay = (trip.budget() - hotelPerNight * trip.numberOfNights()) / trip.numberOfDays();
        // Un peu de marge par element : tous les jours n'ont pas un repas ou une activite chers
        return new Limits(hotelPerNight, perDay, perDay * 0.75, perDay * 0.5 / 2 * 1.5);
    }

    // Les elements sous le prix maximum, ou les minCount moins chers s'il y en a trop peu
    private List<CatalogItem> affordable(List<CatalogItem> items, double max, int minCount) {
        List<CatalogItem> fitting = items.stream().filter(c -> c.price() <= max).toList();
        if (fitting.size() >= Math.min(minCount, items.size())) {
            return fitting;
        }
        return items.stream().sorted(Comparator.comparingDouble(CatalogItem::price)).limit(minCount).toList();
    }

    private String buildPrompt(TripInfo trip, Limits limits, List<CatalogItem> activities,
                               List<CatalogItem> restaurants, List<CatalogItem> hotels) {
        StringBuilder sb = new StringBuilder();
        sb.append("""
                Tu es le planificateur de voyage de TravelMate. Construis un itinéraire réaliste jour par jour.

                Règles :
                - Utilise UNIQUEMENT les éléments des listes ci-dessous, avec leur id. N'invente aucun lieu.
                - Choisis exactement un hébergement (hotelId) pour tout le séjour.
                - Chaque jour : 1 à 3 activités, plus un déjeuner (vers 12:30) et un dîner (vers 19:30) de type RESTAURANT.
                - Ne répète jamais une activité. Évite de répéter un restaurant.
                - Préfère les activités dont la catégorie correspond aux centres d'intérêt.
                - Respecte les limites de dépenses : mélange activités gratuites ou bon marché et activités payantes.
                - Heures au format HH:mm, dans l'ordre de la journée, sans chevauchement.
                - Chaque jour a un titre court en français (6 mots maximum).
                - "note" explique en français, en 12 mots maximum, pourquoi l'élément plaît à ce voyageur.
                  Ne répète pas le nom du lieu dans la note.

                """);
        sb.append("Voyage : ").append(trip.destination()).append(", du ").append(trip.startDate())
          .append(" au ").append(trip.endDate()).append(" (").append(trip.numberOfDays()).append(" jours, ")
          .append(trip.numberOfNights()).append(" nuits)\n")
          .append(String.format(Locale.ROOT, "Budget total : %.0f EUR%n", trip.budget()))
          .append(String.format(Locale.ROOT, "Limites : hébergement au plus %.0f EUR par nuit, ", limits.hotelPerNight()))
          .append(String.format(Locale.ROOT, "activités + repas environ %.0f EUR par jour au total%n", limits.perDay()))
          .append("Centres d'intérêt : ").append(trip.interests().isEmpty() ? "aucun en particulier"
                  : trip.interests().stream().map(Interest::name).collect(Collectors.joining(", ")))
          .append("\n\n");
        appendList(sb, "HÉBERGEMENTS (prix par nuit)", hotels);
        appendList(sb, "ACTIVITÉS (prix par personne, catégorie = centre d'intérêt)", activities);
        appendList(sb, "RESTAURANTS (prix moyen d'un repas)", restaurants);

        // Exemple de reponse avec de vrais ids, pour montrer la forme exacte du JSON
        long hotelId = hotels.isEmpty() ? 0 : hotels.get(0).id();
        long activityId = activities.isEmpty() ? 0 : activities.get(0).id();
        long restaurantId = restaurants.isEmpty() ? 0 : restaurants.get(0).id();
        sb.append("Réponds UNIQUEMENT avec un objet JSON de cette forme, avec les jours numérotés de 1 à ")
          .append(trip.numberOfDays()).append(" :\n")
          .append("{\"hotelId\": ").append(hotelId).append(", \"days\": [{\"dayNumber\": 1, \"title\": \"Premiers pas en ville\", \"items\": [\n")
          .append("  {\"type\": \"ACTIVITE\", \"refId\": ").append(activityId)
          .append(", \"startTime\": \"09:00\", \"endTime\": \"11:00\", \"note\": \"Idéal pour découvrir l'histoire de la ville\"},\n")
          .append("  {\"type\": \"RESTAURANT\", \"refId\": ").append(restaurantId)
          .append(", \"startTime\": \"12:30\", \"endTime\": \"13:30\", \"note\": \"Cuisine locale à petit prix\"}]}]}\n");
        return sb.toString();
    }

    private void appendList(StringBuilder sb, String title, List<CatalogItem> items) {
        sb.append(title).append(" :\n");
        for (CatalogItem c : items) {
            sb.append("- id=").append(c.id()).append(" | ").append(c.name()).append(" | ").append(c.category())
              .append(" | ").append(String.format(Locale.ROOT, "%.0f", c.price())).append(" EUR");
            if (c.durationMinutes() != null) sb.append(" | ").append(c.durationMinutes()).append(" min");
            if (c.rating() != null) sb.append(" | avis ").append(c.rating());
            sb.append('\n');
        }
        sb.append('\n');
    }

    // ---------- 2. Verification de la reponse de l'IA ----------

    private ItineraryPlan validate(AiPlan aiPlan, TripInfo trip, List<CatalogItem> activities,
                                   List<CatalogItem> restaurants, List<CatalogItem> hotels) {
        Map<Long, CatalogItem> activityById = byId(activities);
        Map<Long, CatalogItem> restaurantById = byId(restaurants);

        CatalogItem hotel = aiPlan.hotelId() == null ? null : byId(hotels).get(aiPlan.hotelId());
        if (hotel == null) {
            hotel = pickHotel(trip, hotels);
        }

        Map<Integer, AiDay> aiDays = aiPlan.days() == null ? Map.of() : aiPlan.days().stream()
                .filter(d -> d.dayNumber() != null)
                .collect(Collectors.toMap(AiDay::dayNumber, d -> d, (a, b) -> a));

        Set<Long> usedActivities = new HashSet<>();
        List<Day> days = new ArrayList<>();
        int aiDaysKept = 0;
        for (int n = 1; n <= trip.numberOfDays(); n++) {
            AiDay aiDay = aiDays.get(n);
            List<Slot> slots = new ArrayList<>();
            Set<Long> restaurantsToday = new HashSet<>();
            if (aiDay != null && aiDay.items() != null) {
                for (AiItem ai : aiDay.items()) {
                    ItemType type = toType(ai.type());
                    CatalogItem c = null;
                    if (type == ItemType.ACTIVITE && ai.refId() != null && usedActivities.add(ai.refId())) {
                        c = activityById.get(ai.refId());
                    } else if (type == ItemType.RESTAURANT && ai.refId() != null && restaurantsToday.add(ai.refId())) {
                        c = restaurantById.get(ai.refId());
                    }
                    LocalTime start = parseTime(ai.startTime());
                    if (c == null || start == null) {
                        continue;   // id invente, deja utilise ou heure illisible
                    }
                    LocalTime end = parseTime(ai.endTime());
                    if (end == null || !end.isAfter(start)) {
                        end = start.plusMinutes(duration(c));
                    }
                    slots.add(new Slot(c, start, end, ai.note()));
                }
            }
            if (slots.isEmpty()) {
                days.add(fallbackDay(n, trip, activities, restaurants, usedActivities, hotel));
            } else {
                // Le modele oublie souvent un repas : on complete avec un restaurant abordable
                List<CatalogItem> meals = affordableRestaurants(trip, restaurants, hotel);
                addMissingMeal(slots, LocalTime.of(11, 0), LocalTime.of(16, 30), LocalTime.of(12, 30), meals, restaurantsToday, n);
                addMissingMeal(slots, LocalTime.of(16, 30), LocalTime.of(23, 0), LocalTime.of(19, 30), meals, restaurantsToday, n);
                slots = removeOverlaps(slots);
                String title = aiDay.title() == null || aiDay.title().isBlank() ? "Jour " + n : aiDay.title();
                days.add(new Day(n, title, slots));
                aiDaysKept++;
            }
        }
        log.info("Itineraire IA : {}/{} jours gardes", aiDaysKept, trip.numberOfDays());
        return new ItineraryPlan(hotel, days, aiDaysKept > 0);
    }

    // "ACTIVITE", "Activité" ou "ACTIVITY" -> ACTIVITE ; "restaurant" -> RESTAURANT ; sinon null
    private ItemType toType(String value) {
        if (value == null) {
            return null;
        }
        String v = Normalizer.normalize(value, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toUpperCase();
        if (v.startsWith("ACTIVIT")) return ItemType.ACTIVITE;
        if (v.startsWith("RESTAURANT")) return ItemType.RESTAURANT;
        return null;
    }

    // Ajoute un restaurant a l'heure "at" si aucun repas ne commence entre "from" et "to"
    private void addMissingMeal(List<Slot> slots, LocalTime from, LocalTime to, LocalTime at,
                                List<CatalogItem> meals, Set<Long> restaurantsToday, int dayNumber) {
        boolean hasMeal = slots.stream().anyMatch(s -> s.item().type() == ItemType.RESTAURANT
                && !s.start().isBefore(from) && s.start().isBefore(to));
        if (hasMeal || meals.isEmpty()) {
            return;
        }
        // On tourne dans la liste pour que les repas ajoutes changent d'un jour a l'autre
        for (int i = 0; i < meals.size(); i++) {
            CatalogItem meal = meals.get((dayNumber + i) % meals.size());
            if (restaurantsToday.add(meal.id())) {
                slots.add(slot(meal, at));
                return;
            }
        }
    }

    // Trie la journee et decale un element qui commence avant la fin du precedent
    private List<Slot> removeOverlaps(List<Slot> slots) {
        List<Slot> sorted = new ArrayList<>(slots);
        sorted.sort(Comparator.comparing(Slot::start));
        List<Slot> result = new ArrayList<>();
        LocalTime previousEnd = LocalTime.MIN;
        for (Slot s : sorted) {
            LocalTime start = s.start().isBefore(previousEnd) ? previousEnd : s.start();
            LocalTime end = start.plusMinutes(Duration.between(s.start(), s.end()).toMinutes());
            if (end.isBefore(start)) {
                break;   // depasserait minuit : on arrete la journee
            }
            result.add(new Slot(s.item(), start, end, s.note()));
            previousEnd = end;
        }
        return result;
    }

    private LocalTime parseTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalTime.parse(value.trim(), TIME);
        } catch (Exception e) {
            return null;   // heure mal ecrite par le modele
        }
    }

    // ---------- 3. Plan construit par les regles (Ollama indisponible ou jour oublie) ----------

    ItineraryPlan fallbackPlan(TripInfo trip, List<CatalogItem> activities,
                               List<CatalogItem> restaurants, List<CatalogItem> hotels) {
        CatalogItem hotel = pickHotel(trip, hotels);
        Set<Long> used = new HashSet<>();
        List<Day> days = new ArrayList<>();
        for (int n = 1; n <= trip.numberOfDays(); n++) {
            days.add(fallbackDay(n, trip, activities, restaurants, used, hotel));
        }
        return new ItineraryPlan(hotel, days, false);
    }

    // Activite le matin, dejeuner, activite l'apres-midi, diner
    private Day fallbackDay(int n, TripInfo trip, List<CatalogItem> activities, List<CatalogItem> restaurants,
                            Set<Long> usedActivities, CatalogItem hotel) {
        List<CatalogItem> freeActivities = activities.stream()
                .filter(a -> !usedActivities.contains(a.id()))
                .sorted(Comparator.comparingDouble((CatalogItem a) -> -score(a, trip)))
                .limit(2)
                .toList();
        freeActivities.forEach(a -> usedActivities.add(a.id()));

        List<CatalogItem> meals = affordableRestaurants(trip, restaurants, hotel);
        List<Slot> slots = new ArrayList<>();
        if (!freeActivities.isEmpty()) slots.add(slot(freeActivities.get(0), LocalTime.of(9, 0)));
        if (!meals.isEmpty()) slots.add(slot(meals.get((2 * n) % meals.size()), LocalTime.of(12, 30)));
        if (freeActivities.size() > 1) slots.add(slot(freeActivities.get(1), LocalTime.of(14, 30)));
        if (!meals.isEmpty()) slots.add(slot(meals.get((2 * n + 1) % meals.size()), LocalTime.of(19, 30)));

        String title = freeActivities.isEmpty() ? "Journée libre à " + trip.destination()
                : freeActivities.stream().map(a -> label(a.category())).distinct().collect(Collectors.joining(" et "));
        return new Day(n, title, slots);
    }

    // L'hebergement le mieux note qui coute au plus 45 % du budget, sinon le moins cher
    CatalogItem pickHotel(TripInfo trip, List<CatalogItem> hotels) {
        if (hotels.isEmpty()) {
            return null;
        }
        double maxPerNight = trip.budget() * 0.45 / trip.numberOfNights();
        return hotels.stream()
                .filter(h -> h.price() <= maxPerNight)
                .max(Comparator.comparingDouble(h -> h.rating() == null ? 0 : h.rating()))
                .orElseGet(() -> hotels.stream().min(Comparator.comparingDouble(CatalogItem::price)).get());
    }

    // Restaurants qui tiennent dans environ 35 % du budget restant apres l'hebergement, les mieux notes d'abord
    private List<CatalogItem> affordableRestaurants(TripInfo trip, List<CatalogItem> restaurants, CatalogItem hotel) {
        double hotelCost = hotel == null ? 0 : hotel.price() * trip.numberOfNights();
        double maxPerMeal = Math.max(0, trip.budget() - hotelCost) * 0.35 / (2.0 * trip.numberOfDays());
        List<CatalogItem> fitting = restaurants.stream()
                .filter(r -> r.price() <= maxPerMeal)
                .sorted(Comparator.comparingDouble((CatalogItem r) -> r.rating() == null ? 0 : -r.rating()))
                .toList();
        if (!fitting.isEmpty()) {
            return fitting;
        }
        return restaurants.stream().sorted(Comparator.comparingDouble(CatalogItem::price)).limit(2).toList();
    }

    // Une activite qui correspond a un centre d'interet du voyage passe avant les autres
    private double score(CatalogItem a, TripInfo trip) {
        boolean matches = trip.interests().stream().anyMatch(i -> i.name().equals(a.category()));
        return (matches ? 2 : 0) + (a.rating() == null ? 0 : a.rating());
    }

    private Slot slot(CatalogItem c, LocalTime start) {
        return new Slot(c, start, start.plusMinutes(duration(c)), null);
    }

    private int duration(CatalogItem c) {
        if (c.durationMinutes() != null) {
            return c.durationMinutes();
        }
        return c.type() == ItemType.RESTAURANT ? 60 : 120;
    }

    private static Map<Long, CatalogItem> byId(List<CatalogItem> items) {
        return items.stream().collect(Collectors.toMap(CatalogItem::id, c -> c, (a, b) -> a));
    }

    // "VIE_NOCTURNE" -> "Vie nocturne"
    private static String label(String category) {
        if (category == null || category.isEmpty()) {
            return "";
        }
        String text = category.replace('_', ' ').toLowerCase();
        return Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }

    // ---------- Forme du JSON renvoye par l'IA ----------

    // Limites de prix (EUR) envoyees au modele
    private record Limits(double hotelPerNight, double perDay, double activity, double meal) {
    }

    record AiPlan(Long hotelId, List<AiDay> days) {
    }

    record AiDay(Integer dayNumber, String title, List<AiItem> items) {
    }

    record AiItem(String type, Long refId, String startTime, String endTime, String note) {
    }
}
