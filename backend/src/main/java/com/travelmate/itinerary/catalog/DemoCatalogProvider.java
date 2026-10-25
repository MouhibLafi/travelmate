package com.travelmate.itinerary.catalog;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.travelmate.itinerary.entity.ItemType;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;

/*
 * Catalogue de demonstration (resources/itinerary/catalogue-demo.json) : Rome, Paris, Tunis, Djerba, Marrakech.
 * A remplacer par les vrais modules 3, 4 et 5 (voir CatalogProvider).
 * La destination d'un voyage est comparee sans accents ni majuscules, sur la ville seulement :
 * "Rome, Italie" trouve "Rome".
 */
@Component
public class DemoCatalogProvider implements CatalogProvider {

    private final List<Entry> entries = new ArrayList<>();

    private record Entry(String city, CatalogItem item) {
    }

    public DemoCatalogProvider() throws IOException {
        try (InputStream in = new ClassPathResource("itinerary/catalogue-demo.json").getInputStream()) {
            JsonNode root = new ObjectMapper().readTree(in);
            load(root.path("activities"), ItemType.ACTIVITE);
            load(root.path("restaurants"), ItemType.RESTAURANT);
            load(root.path("hotels"), ItemType.HEBERGEMENT);
        }
    }

    @Override
    public List<CatalogItem> getActivities(String destination) {
        return find(destination, ItemType.ACTIVITE);
    }

    @Override
    public List<CatalogItem> getRestaurants(String destination) {
        return find(destination, ItemType.RESTAURANT);
    }

    @Override
    public List<CatalogItem> getHotels(String destination) {
        return find(destination, ItemType.HEBERGEMENT);
    }

    private List<CatalogItem> find(String destination, ItemType type) {
        String city = city(destination);
        return entries.stream()
                .filter(e -> e.city().equals(city) && e.item().type() == type)
                .map(Entry::item)
                .toList();
    }

    private void load(JsonNode list, ItemType type) {
        for (JsonNode n : list) {
            CatalogItem item = new CatalogItem(n.path("id").asLong(), type, n.path("name").asText(),
                    n.path("category").asText(), n.path("price").asDouble(),
                    n.path("durationMinutes").isNumber() ? n.path("durationMinutes").asInt() : null,
                    n.path("rating").isNumber() ? n.path("rating").asDouble() : null);
            entries.add(new Entry(city(n.path("destination").asText()), item));
        }
    }

    // "Rome, Italie" -> "rome", "Aïn Draham" -> "ain draham"
    static String city(String destination) {
        if (destination == null) {
            return "";
        }
        String city = destination.split(",")[0].trim().toLowerCase();
        return Normalizer.normalize(city, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
    }
}
