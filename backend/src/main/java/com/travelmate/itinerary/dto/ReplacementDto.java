package com.travelmate.itinerary.dto;

import com.travelmate.itinerary.entity.ItemType;

// Un remplacement fait par l'optimiseur de budget
public record ReplacementDto(Long itemId,
                             int dayNumber,
                             ItemType type,
                             String oldName,
                             String newName,
                             double oldCost,
                             double newCost,
                             String reason) {
}
