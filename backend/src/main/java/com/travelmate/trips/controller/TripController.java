package com.travelmate.trips.controller;

import com.travelmate.trips.dto.TripAiRequest;
import com.travelmate.trips.dto.TripRequest;
import com.travelmate.trips.dto.TripResponse;
import com.travelmate.trips.dto.TripSuggestion;
import com.travelmate.trips.service.TripAiService;
import com.travelmate.trips.service.TripService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

// Les voyages de l'utilisateur CONNECTE.
// Rien a configurer pour la securite : le JWT du module 1 protege deja toutes les routes /api/**.
@RestController
@RequestMapping("/api/trips")
public class TripController {

    private final TripService tripService;
    private final TripAiService tripAiService;

    public TripController(TripService tripService, TripAiService tripAiService) {
        this.tripService = tripService;
        this.tripAiService = tripAiService;
    }

    // POST /api/trips/ai/fill-form   body : { "text": "5 jours à Rome, 800 €" }
    // Tache IA : renvoie le formulaire pre-rempli. Rien n'est enregistre ici :
    // l'utilisateur verifie, puis le front appelle POST /api/trips.
    @PostMapping("/ai/fill-form")
    public TripSuggestion fillForm(@Valid @RequestBody TripAiRequest request) {
        return tripAiService.fillForm(request.text());
    }

    // GET /api/trips
    @GetMapping
    public List<TripResponse> myTrips(Principal principal) {
        return tripService.findMyTrips(principal.getName());
    }

    // POST /api/trips
    @PostMapping
    public TripResponse create(Principal principal, @Valid @RequestBody TripRequest request) {
        return tripService.create(principal.getName(), request);
    }

    // PUT /api/trips/3
    @PutMapping("/{id}")
    public TripResponse update(Principal principal, @PathVariable Long id, @Valid @RequestBody TripRequest request) {
        return tripService.update(principal.getName(), id, request);
    }

    // DELETE /api/trips/3
    @DeleteMapping("/{id}")
    public void delete(Principal principal, @PathVariable Long id) {
        tripService.delete(principal.getName(), id);
    }
}
