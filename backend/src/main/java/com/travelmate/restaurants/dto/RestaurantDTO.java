package com.travelmate.restaurants.dto;

import java.util.List;

public record RestaurantDTO(
        Long id,
        String nom,
        String cuisine,
        String description,
        String adresse,
        String ville,
        double prixMoyen,
        String fourchettePrix,
        List<String> optionsAlimentaires,
        double noteMoyenne,
        int nombreAvis,
        String imageUrl
) {
}
