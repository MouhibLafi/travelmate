package com.travelmate.itinerary.catalog;

import com.travelmate.itinerary.entity.ItemType;

/*
 * Forme commune d'une activite (module 3), d'un restaurant (module 4) ou d'un hebergement (module 5).
 * price : par personne pour une activite, prix moyen d'un repas pour un restaurant, par nuit pour un hebergement.
 * category : un Interest du module 1 pour une activite (ex. HISTOIRE), un type libre sinon (ex. "auberge").
 */
public record CatalogItem(Long id,
                          ItemType type,
                          String name,
                          String category,
                          double price,
                          Integer durationMinutes,
                          Double rating) {
}
