package com.travelmate.restaurants.dto;

import java.time.LocalDateTime;

public record AvisDTO(
        Long id,
        Long restaurantId,
        String auteur,
        int note,
        String commentaire,
        String sentiment,
        double scoreSentiment,
        String sourceSentiment,
        LocalDateTime dateCreation
) {
}
