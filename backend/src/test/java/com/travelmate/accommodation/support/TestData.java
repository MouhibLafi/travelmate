package com.travelmate.accommodation.support;

import com.travelmate.accommodation.dto.AccommodationSummary;
import com.travelmate.accommodation.entity.AccommodationType;
import com.travelmate.accommodation.entity.Amenity;

import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.List;

public final class TestData {

    private TestData() {
    }

    public static AccommodationSummary summary(long id, String city, AccommodationType type, int stars, double rating,
                                               double price, Amenity... amenities) {
        return new AccommodationSummary(id, "Listing " + id, type, city, "Italy", stars, rating,
                BigDecimal.valueOf(price).setScale(2), 4,
                amenities.length == 0 ? EnumSet.noneOf(Amenity.class) : EnumSet.copyOf(List.of(amenities)),
                null);
    }
}
