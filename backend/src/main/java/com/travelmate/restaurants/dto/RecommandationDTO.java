package com.travelmate.restaurants.dto;

public record RecommandationDTO(
        RestaurantDTO restaurant,
        double score,
        boolean dansBudget,
        String raison
) {
}
