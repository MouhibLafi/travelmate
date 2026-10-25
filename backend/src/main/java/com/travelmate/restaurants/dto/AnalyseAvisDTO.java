package com.travelmate.restaurants.dto;

import java.util.List;

public record AnalyseAvisDTO(
        Long restaurantId,
        int nombreAvis,
        double pourcentagePositifs,
        double pourcentageNegatifs,
        double pourcentageNeutres,
        double noteMoyenne,
        List<String> pointsForts,
        List<String> pointsFaibles
) {
}
