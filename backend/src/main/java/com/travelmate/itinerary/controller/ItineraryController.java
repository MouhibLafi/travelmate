package com.travelmate.itinerary.controller;

import com.travelmate.itinerary.catalog.CatalogItem;
import com.travelmate.itinerary.dto.*;
import com.travelmate.itinerary.entity.ItemType;
import com.travelmate.itinerary.service.BudgetOptimizerService;
import com.travelmate.itinerary.service.ItineraryService;
import com.travelmate.itinerary.service.TripPlanningService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

// Les itineraires de l'utilisateur CONNECTE. Les voyages a choisir viennent du module 2 : GET /api/trips.
// Rien a configurer pour la securite : le JWT du module 1 protege deja toutes les routes /api/**.
@RestController
@RequestMapping("/api/itinerary")
public class ItineraryController {

    private final ItineraryService itineraryService;
    private final BudgetOptimizerService optimizerService;
    private final TripPlanningService planningService;

    public ItineraryController(ItineraryService itineraryService, BudgetOptimizerService optimizerService,
                               TripPlanningService planningService) {
        this.itineraryService = itineraryService;
        this.optimizerService = optimizerService;
        this.planningService = planningService;
    }

    // GET /api/itinerary
    @GetMapping
    public List<ItineraryDto> myItineraries(Principal principal) {
        return itineraryService.findMine(principal.getName());
    }

    // POST /api/itinerary/generate   body : { "tripId": 3 }
    // Tache IA : genere (ou regenere) l'itineraire, puis remplace des elements par moins cher s'il depasse le budget
    @PostMapping("/generate")
    public GenerationResultDto generate(Principal principal, @Valid @RequestBody GenerateRequest request) {
        return planningService.plan(principal.getName(), request.tripId());
    }

    // GET /api/itinerary/5
    @GetMapping("/{id}")
    public ItineraryDto get(Principal principal, @PathVariable Long id) {
        return itineraryService.getById(principal.getName(), id);
    }

    // DELETE /api/itinerary/5
    @DeleteMapping("/{id}")
    public void delete(Principal principal, @PathVariable Long id) {
        itineraryService.delete(principal.getName(), id);
    }

    // GET /api/itinerary/5/catalog?type=ACTIVITE : pour ajouter un element a la main
    @GetMapping("/{id}/catalog")
    public List<CatalogItem> catalog(Principal principal, @PathVariable Long id, @RequestParam ItemType type) {
        return itineraryService.getCatalog(principal.getName(), id, type);
    }

    // POST /api/itinerary/5/days/12/items
    @PostMapping("/{id}/days/{dayId}/items")
    public ItineraryDto addItem(Principal principal, @PathVariable Long id, @PathVariable Long dayId,
                                @Valid @RequestBody ItemRequest request) {
        return itineraryService.addItem(principal.getName(), id, dayId, request);
    }

    // PUT /api/itinerary/items/40
    @PutMapping("/items/{itemId}")
    public ItineraryDto updateItem(Principal principal, @PathVariable Long itemId, @Valid @RequestBody ItemRequest request) {
        return itineraryService.updateItem(principal.getName(), itemId, request);
    }

    // DELETE /api/itinerary/items/40
    @DeleteMapping("/items/{itemId}")
    public ItineraryDto deleteItem(Principal principal, @PathVariable Long itemId) {
        return itineraryService.deleteItem(principal.getName(), itemId);
    }

    // POST /api/itinerary/5/optimize
    // Tache IA : remplace des elements par des alternatives moins cheres quand le budget est depasse
    @PostMapping("/{id}/optimize")
    public OptimizationResultDto optimize(Principal principal, @PathVariable Long id) {
        return optimizerService.optimize(principal.getName(), id);
    }
}
