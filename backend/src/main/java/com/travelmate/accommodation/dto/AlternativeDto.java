package com.travelmate.accommodation.dto;

import com.travelmate.accommodation.entity.AccommodationType;
import com.travelmate.accommodation.entity.Amenity;

import java.math.BigDecimal;
import java.util.Set;

public record AlternativeDto(
        Long id,
        String name,
        AccommodationType type,
        String city,
        Integer stars,
        Double rating,
        BigDecimal pricePerNight,
        Set<Amenity> amenities,
        String mainImageUrl,
        BigDecimal savingsPerNight,
        double savingsPercent,
        double similarityScore,
        String reason
) {
}
