package com.travelmate.itinerary.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.travelmate.ai.OllamaClient;
import com.travelmate.itinerary.catalog.CatalogItem;
import com.travelmate.itinerary.catalog.CatalogProvider;
import com.travelmate.itinerary.dto.ItineraryDto;
import com.travelmate.itinerary.dto.OptimizationResultDto;
import com.travelmate.itinerary.dto.ReplacementDto;
import com.travelmate.itinerary.entity.ItemType;
import com.travelmate.itinerary.entity.Itinerary;
import com.travelmate.itinerary.entity.ItineraryItem;
import com.travelmate.trips.repository.TripRepository;
import com.travelmate.users.entity.Interest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/*
 * TACHE IA DU MODULE 6 (2/2) : si l'itineraire depasse le budget, remplacer des elements par moins cher.
 *   1. Java liste, pour chaque element, les alternatives moins cheres du catalogue
 *   2. l'IA (llama3) choisit les remplacements en gardant les centres d'interet, et explique chacun
 *   3. Java applique seulement les remplacements valides, un par un, et S'ARRETE des que le budget est tenu
 *      (jusqu'a 3 tours)
 *   4. si ce n'est pas assez (ou si Ollama ne repond pas), un algorithme glouton termine
 *   5. dernier recours : retirer une activite payante
 */
@Service
public class BudgetOptimizerService {

    private static final Logger log = LoggerFactory.getLogger(BudgetOptimizerService.class);
    private static final int MAX_AI_ROUNDS = 3;
    private static final int MAX_ALTERNATIVES = 4;
    private static final List<ItemType> REPLACEABLE = List.of(ItemType.HEBERGEMENT, ItemType.ACTIVITE, ItemType.RESTAURANT);

    private final ItineraryService itineraryService;
    private final CatalogProvider catalogProvider;
    private final TripRepository tripRepository;   // module 2, pour les centres d'interet
    private final OllamaClient ollamaClient;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    public BudgetOptimizerService(ItineraryService itineraryService, CatalogProvider catalogProvider,
                                  TripRepository tripRepository, OllamaClient ollamaClient) {
        this.itineraryService = itineraryService;
        this.catalogProvider = catalogProvider;
        this.tripRepository = tripRepository;
        this.ollamaClient = ollamaClient;
    }

    @Transactional
    public OptimizationResultDto optimize(String email, Long itineraryId) {
        Itinerary it = itineraryService.findMyItinerary(email, itineraryId);
        it.recalculateTotal();
        double before = it.getTotalEstimatedCost();
        if (!it.isOverBudget()) {
            return result(it, before, List.of(), false, "L'itinéraire tient déjà dans le budget.");
        }

        Map<ItemType, List<CatalogItem>> catalog = new EnumMap<>(ItemType.class);
        for (ItemType type : REPLACEABLE) {
            catalog.put(type, catalogProvider.getByType(type, it.getDestination()));
        }
        List<String> interests = interests(it);
        List<ReplacementDto> replacements = new ArrayList<>();
        boolean aiUsed = false;

        for (int round = 1; round <= MAX_AI_ROUNDS && it.isOverBudget(); round++) {
            Map<ItineraryItem, List<CatalogItem>> alternatives = cheaperAlternatives(it, catalog, MAX_ALTERNATIVES);
            if (alternatives.isEmpty()) {
                break;
            }
            try {
                String answer = ollamaClient.generateJson(buildPrompt(it, interests, alternatives));
                int applied = applyAiSwaps(it, objectMapper.readValue(answer, AiSwaps.class), alternatives, replacements);
                log.info("Optimiseur de budget, tour {} : {} remplacement(s) de l'IA", round, applied);
                if (applied == 0) {
                    break;
                }
                aiUsed = true;
            } catch (Exception e) {
                log.warn("Optimisation par l'IA impossible, algorithme glouton : {}", e.getMessage());
                break;
            }
        }

        while (it.isOverBudget() && greedyStep(it, catalog, replacements)) {
            // chaque etape baisse strictement le total : la boucle se termine toujours
        }
        while (it.isOverBudget() && removeActivityStep(it, replacements)) {
            // dernier recours quand il n'y a plus d'alternative moins chere
        }

        double after = it.getTotalEstimatedCost();
        String message = it.isOverBudget()
                ? String.format(Locale.ROOT, "%.0f € économisés, mais encore %.0f € au-dessus du budget. "
                        + "Augmentez le budget ou choisissez un hébergement moins cher.", before - after, after - it.getBudget())
                : String.format(Locale.ROOT, "%.0f € économisés avec %d changement(s). L'itinéraire tient maintenant dans le budget.",
                        before - after, replacements.size());
        return result(it, before, replacements, aiUsed, message);
    }

    // ---------- Alternatives ----------

    /*
     * Pour chaque element du catalogue : les elements moins chers du meme type.
     * Une activite deja prevue est exclue ; un restaurant deja prevu passe en dernier ; meme categorie d'abord.
     */
    private Map<ItineraryItem, List<CatalogItem>> cheaperAlternatives(Itinerary it, Map<ItemType, List<CatalogItem>> catalog,
                                                                      int limit) {
        Set<Long> usedActivities = new HashSet<>();
        Set<Long> usedRestaurants = new HashSet<>();
        for (ItineraryItem i : it.allItems()) {
            if (i.getType() == ItemType.ACTIVITE) usedActivities.add(i.getRefId());
            if (i.getType() == ItemType.RESTAURANT) usedRestaurants.add(i.getRefId());
        }

        Map<ItineraryItem, List<CatalogItem>> result = new LinkedHashMap<>();
        for (ItineraryItem item : it.allItems()) {
            if (item.getRefId() == null || !catalog.containsKey(item.getType())) {
                continue;
            }
            List<CatalogItem> alts = catalog.get(item.getType()).stream()
                    .filter(c -> c.price() < item.getUnitPrice())
                    .filter(c -> item.getType() != ItemType.ACTIVITE || !usedActivities.contains(c.id()))
                    .sorted(Comparator
                            .comparing((CatalogItem c) -> usedRestaurants.contains(c.id()))
                            .thenComparing(c -> !Objects.equals(c.category(), item.getCategory()))
                            .thenComparing(c -> c.rating() == null ? 0 : -c.rating()))
                    .limit(limit)
                    .toList();
            if (!alts.isEmpty()) {
                result.put(item, alts);
            }
        }
        return result;
    }

    // ---------- IA ----------

    private String buildPrompt(Itinerary it, List<String> interests, Map<ItineraryItem, List<CatalogItem>> alternatives) {
        StringBuilder sb = new StringBuilder();
        sb.append("""
                Tu es l'optimiseur de budget de TravelMate. L'itinéraire dépasse le budget.
                Remplace des éléments par des alternatives moins chères pour tenir le budget,
                en gardant un voyage agréable et adapté aux centres d'intérêt du voyageur.
                Règles :
                - Utilise UNIQUEMENT les itemId et les id d'alternatives donnés ci-dessous.
                - Fais le moins de remplacements possible, de préférence dans la même catégorie.
                - Pour chaque remplacement, donne une raison courte en français (15 mots maximum).

                """);
        sb.append("Destination : ").append(it.getDestination()).append('\n')
          .append(String.format(Locale.ROOT, "Budget : %.0f EUR%n", it.getBudget()))
          .append(String.format(Locale.ROOT, "Total actuel : %.0f EUR%n", it.getTotalEstimatedCost()))
          .append(String.format(Locale.ROOT, "Économie nécessaire : au moins %.0f EUR%n", it.getTotalEstimatedCost() - it.getBudget()))
          .append("Centres d'intérêt : ").append(interests.isEmpty() ? "aucun en particulier" : String.join(", ", interests))
          .append("\n\nÉléments remplaçables :\n");
        alternatives.forEach((item, alts) -> {
            sb.append("- itemId=").append(item.getId()).append(" | jour ").append(item.getDay().getDayNumber())
              .append(" | ").append(item.getType()).append(" | ").append(item.getName())
              .append(" | ").append(item.getCategory())
              .append(String.format(Locale.ROOT, " | %.0f EUR", item.getUnitPrice()));
            if (item.getQuantity() > 1) {
                sb.append(String.format(Locale.ROOT, " x %d = %.0f EUR", item.getQuantity(), item.getEstimatedCost()));
            }
            sb.append("\n  alternatives :");
            for (CatalogItem c : alts) {
                sb.append(" [id=").append(c.id()).append(' ').append(c.name()).append(", ").append(c.category())
                  .append(String.format(Locale.ROOT, ", %.0f EUR", c.price()));
                if (c.rating() != null) sb.append(", avis ").append(c.rating());
                sb.append(']');
            }
            sb.append('\n');
        });
        sb.append("""

                Réponds UNIQUEMENT avec un objet JSON de cette forme :
                {"replacements": [{"itemId": 12, "newRefId": 305, "reason": "Plus simple mais bien situé, économise 160 €"}]}
                """);
        return sb.toString();
    }

    // Applique les remplacements valides un par un, et s'arrete des que le budget est tenu
    private int applyAiSwaps(Itinerary it, AiSwaps swaps, Map<ItineraryItem, List<CatalogItem>> alternatives,
                             List<ReplacementDto> replacements) {
        if (swaps == null || swaps.replacements() == null) {
            return 0;
        }
        Map<Long, ItineraryItem> itemsById = new HashMap<>();
        alternatives.keySet().forEach(i -> itemsById.put(i.getId(), i));
        Set<Long> usedNewActivities = new HashSet<>();
        int applied = 0;
        for (AiSwap swap : swaps.replacements()) {
            if (!it.isOverBudget()) {
                break;   // le modele propose souvent plus de remplacements que necessaire
            }
            ItineraryItem item = itemsById.remove(swap.itemId());   // chaque element au plus une fois par tour
            if (item == null) {
                continue;
            }
            Optional<CatalogItem> alt = alternatives.get(item).stream()
                    .filter(c -> c.id().equals(swap.newRefId()))
                    .findFirst();
            if (alt.isEmpty()) {
                continue;   // alternative inventee par le modele
            }
            if (item.getType() == ItemType.ACTIVITE && !usedNewActivities.add(alt.get().id())) {
                continue;
            }
            replace(item, alt.get(), swap.reason(), replacements);
            it.recalculateTotal();
            applied++;
        }
        return applied;
    }

    // ---------- Algorithme glouton (sans IA) ----------

    /*
     * Si un seul remplacement suffit a tenir le budget, on prend celui qui economise le MOINS (le plus petit sacrifice).
     * Sinon, celui qui economise le PLUS. Renvoie false quand plus rien n'est remplacable.
     */
    private boolean greedyStep(Itinerary it, Map<ItemType, List<CatalogItem>> catalog, List<ReplacementDto> replacements) {
        double overspend = it.getTotalEstimatedCost() - it.getBudget();
        ItineraryItem bestItem = null;
        CatalogItem bestAlt = null;
        double bestSaving = 0;
        boolean bestCloses = false;

        for (Map.Entry<ItineraryItem, List<CatalogItem>> e : cheaperAlternatives(it, catalog, Integer.MAX_VALUE).entrySet()) {
            ItineraryItem item = e.getKey();
            for (CatalogItem alt : e.getValue()) {
                double saving = (item.getUnitPrice() - alt.price()) * item.getQuantity();
                boolean closes = saving >= overspend;
                boolean better = bestItem == null
                        || (closes && !bestCloses)
                        || (closes && saving < bestSaving)
                        || (!closes && !bestCloses && saving > bestSaving);
                if (better) {
                    bestItem = item;
                    bestAlt = alt;
                    bestSaving = saving;
                    bestCloses = closes;
                }
            }
        }
        if (bestItem == null) {
            return false;
        }
        replace(bestItem, bestAlt, String.format(Locale.ROOT, "Option similaire moins chère, économise %.0f €", bestSaving),
                replacements);
        it.recalculateTotal();
        return true;
    }

    // Retire l'activite payante la moins chere qui suffit, sinon la plus chere
    private boolean removeActivityStep(Itinerary it, List<ReplacementDto> replacements) {
        double overspend = it.getTotalEstimatedCost() - it.getBudget();
        List<ItineraryItem> paid = it.allItems().stream()
                .filter(i -> i.getType() == ItemType.ACTIVITE && i.getEstimatedCost() > 0)
                .sorted(Comparator.comparingDouble(ItineraryItem::getEstimatedCost))
                .toList();
        if (paid.isEmpty()) {
            return false;
        }
        ItineraryItem victim = paid.stream().filter(i -> i.getEstimatedCost() >= overspend).findFirst()
                .orElse(paid.get(paid.size() - 1));
        replacements.add(new ReplacementDto(victim.getId(), victim.getDay().getDayNumber(), victim.getType(),
                victim.getName(), "(retirée, temps libre)", victim.getEstimatedCost(), 0,
                "Plus d'alternative moins chère : activité retirée pour tenir le budget"));
        victim.getDay().getItems().remove(victim);
        it.recalculateTotal();
        return true;
    }

    // ---------- Outils ----------

    private void replace(ItineraryItem item, CatalogItem alt, String reason, List<ReplacementDto> replacements) {
        double oldCost = item.getEstimatedCost();
        String oldName = item.getName();
        item.setRefId(alt.id());
        item.setName(alt.name());
        item.setCategory(alt.category());
        item.setPricing(alt.price(), item.getQuantity());
        if (item.getStartTime() != null && alt.durationMinutes() != null && item.getType() != ItemType.HEBERGEMENT) {
            item.setEndTime(item.getStartTime().plusMinutes(alt.durationMinutes()));
        }
        item.setNotes("Remplace " + oldName + " : " + reason);
        replacements.add(new ReplacementDto(item.getId(), item.getDay().getDayNumber(), item.getType(),
                oldName, alt.name(), oldCost, item.getEstimatedCost(), reason));
    }

    private List<String> interests(Itinerary it) {
        return tripRepository.findById(it.getTripId())
                .map(trip -> trip.getInterests().stream().map(Interest::name).collect(Collectors.toList()))
                .orElse(List.of());
    }

    private OptimizationResultDto result(Itinerary it, double before, List<ReplacementDto> replacements,
                                         boolean aiUsed, String message) {
        return new OptimizationResultDto(ItineraryDto.from(it), it.getBudget(), before, it.getTotalEstimatedCost(),
                !it.isOverBudget(), aiUsed, replacements, message);
    }

    // ---------- Forme du JSON renvoye par l'IA ----------

    record AiSwaps(List<AiSwap> replacements) {
    }

    record AiSwap(Long itemId, Long newRefId, String reason) {
    }
}
