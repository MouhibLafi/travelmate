package com.travelmate.itinerary.service;

import com.travelmate.itinerary.dto.GenerationResultDto;
import com.travelmate.itinerary.dto.ItineraryDto;
import com.travelmate.itinerary.dto.OptimizationResultDto;
import org.springframework.stereotype.Service;

/*
 * Parcours IA complet du module 6 : generer l'itineraire puis, s'il depasse le budget,
 * remplacer AUTOMATIQUEMENT des elements par des alternatives moins cheres (comme le demande le sujet).
 */
@Service
public class TripPlanningService {

    private final ItineraryService itineraryService;
    private final BudgetOptimizerService optimizerService;

    public TripPlanningService(ItineraryService itineraryService, BudgetOptimizerService optimizerService) {
        this.itineraryService = itineraryService;
        this.optimizerService = optimizerService;
    }

    public GenerationResultDto plan(String email, Long tripId) {
        ItineraryDto generated = itineraryService.generate(email, tripId);
        if (!generated.overBudget()) {
            return new GenerationResultDto(generated, null);
        }
        OptimizationResultDto optimization = optimizerService.optimize(email, generated.id());
        return new GenerationResultDto(optimization.itinerary(), optimization);
    }
}
