package com.travelmate.itinerary.catalog;

import com.travelmate.itinerary.entity.ItemType;

import java.util.List;
import java.util.Optional;

/*
 * Acces aux activites (module 3), restaurants (module 4) et hebergements (module 5) d'une destination.
 * INTEGRATION : tant que ces modules n'existent pas, DemoCatalogProvider lit un catalogue de demonstration.
 * Ensuite, il suffit d'ecrire une implementation qui appelle leurs services et de supprimer DemoCatalogProvider.
 */
public interface CatalogProvider {

    List<CatalogItem> getActivities(String destination);

    List<CatalogItem> getRestaurants(String destination);

    List<CatalogItem> getHotels(String destination);

    default List<CatalogItem> getByType(ItemType type, String destination) {
        return switch (type) {
            case ACTIVITE -> getActivities(destination);
            case RESTAURANT -> getRestaurants(destination);
            case HEBERGEMENT -> getHotels(destination);
            default -> List.of();
        };
    }

    default Optional<CatalogItem> find(ItemType type, Long id, String destination) {
        return getByType(type, destination).stream().filter(c -> c.id().equals(id)).findFirst();
    }
}
