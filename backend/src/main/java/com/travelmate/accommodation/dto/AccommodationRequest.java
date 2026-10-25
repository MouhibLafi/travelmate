package com.travelmate.accommodation.dto;

import com.travelmate.accommodation.entity.AccommodationType;
import com.travelmate.accommodation.entity.Amenity;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

public record AccommodationRequest(
        @NotBlank @Size(max = 150) String name,
        @Size(max = 4000) String description,
        @NotNull AccommodationType type,
        @NotBlank @Size(max = 100) String city,
        @NotBlank @Size(max = 100) String country,
        @Size(max = 255) String address,
        @NotNull @Min(1) @Max(5) Integer stars,
        @NotNull @DecimalMin("0.0") @DecimalMax("5.0") Double rating,
        @NotNull @DecimalMin(value = "1.00") @DecimalMax("100000.00") @Digits(integer = 8, fraction = 2) BigDecimal pricePerNight,
        @NotNull @Min(1) @Max(30) Integer maxGuests,
        Long destinationId,
        Set<Amenity> amenities,
        @Size(max = 10) List<@NotBlank @Size(max = 500) String> imageUrls
) {
}
