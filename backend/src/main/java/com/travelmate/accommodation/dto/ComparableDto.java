package com.travelmate.accommodation.dto;

import com.travelmate.accommodation.entity.AccommodationType;

import java.math.BigDecimal;

public record ComparableDto(
        Long id,
        String name,
        String city,
        AccommodationType type,
        Integer stars,
        Double rating,
        int amenitiesCount,
        BigDecimal pricePerNight,
        double similarityScore
) {
}
