package com.travelmate.itinerary.dto;

// Resultat de la generation. optimization vaut null si l'itineraire genere tenait deja dans le budget.
public record GenerationResultDto(ItineraryDto itinerary, OptimizationResultDto optimization) {
}
