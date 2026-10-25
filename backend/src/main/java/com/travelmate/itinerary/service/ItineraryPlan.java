package com.travelmate.itinerary.service;

import com.travelmate.itinerary.catalog.CatalogItem;

import java.time.LocalTime;
import java.util.List;

// Resultat du planificateur, avant d'etre enregistre en entites
public record ItineraryPlan(CatalogItem hotel, List<Day> days, boolean aiUsed) {

    public record Day(int dayNumber, String title, List<Slot> slots) {
    }

    // note : la phrase de l'IA qui explique le choix (null pour un plan construit par les regles)
    public record Slot(CatalogItem item, LocalTime start, LocalTime end, String note) {
    }
}
