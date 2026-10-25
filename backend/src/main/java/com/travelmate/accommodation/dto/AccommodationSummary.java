package com.travelmate.accommodation.dto;

import com.travelmate.accommodation.entity.AccommodationType;
import com.travelmate.accommodation.entity.Amenity;

import java.math.BigDecimal;
import java.util.Set;

/** Lightweight view used by the catalog grid and by the price analysis. */
public record AccommodationSummary(
        Long id,
        String name,
        AccommodationType type,
        String city,
        String country,
        Integer stars,
        Double rating,
        BigDecimal pricePerNight,
        Integer maxGuests,
        Set<Amenity> amenities,
        String mainImageUrl
) {
}
