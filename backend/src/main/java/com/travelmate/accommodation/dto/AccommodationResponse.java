package com.travelmate.accommodation.dto;

import com.travelmate.accommodation.entity.AccommodationType;
import com.travelmate.accommodation.entity.Amenity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

public record AccommodationResponse(
        Long id,
        String name,
        String description,
        AccommodationType type,
        String city,
        String country,
        String address,
        Integer stars,
        Double rating,
        BigDecimal pricePerNight,
        Integer maxGuests,
        Long destinationId,
        Set<Amenity> amenities,
        List<String> imageUrls,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
