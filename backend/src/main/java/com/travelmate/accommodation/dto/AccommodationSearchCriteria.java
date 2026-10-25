package com.travelmate.accommodation.dto;

import com.travelmate.accommodation.entity.AccommodationType;
import com.travelmate.accommodation.entity.Amenity;
import jakarta.validation.constraints.*;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

/** Query parameters of GET /api/accommodations. All filters are optional. */
@Data
public class AccommodationSearchCriteria {

    /** Free text matched against name, city and country. */
    @Size(max = 100)
    private String q;

    @Size(max = 100)
    private String city;

    private AccommodationType type;

    @DecimalMin("0")
    private BigDecimal minPrice;

    @DecimalMin("0")
    private BigDecimal maxPrice;

    @Min(1) @Max(5)
    private Integer minStars;

    @DecimalMin("0") @DecimalMax("5")
    private Double minRating;

    /** Accommodation must offer ALL of these (e.g. amenities=WIFI,POOL). */
    private Set<Amenity> amenities;

    @Min(1) @Max(30)
    private Integer guests;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate checkIn;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate checkOut;

    @AssertTrue(message = "checkIn and checkOut must be provided together and checkOut must be after checkIn")
    public boolean isDateRangeValid() {
        if (checkIn == null && checkOut == null) {
            return true;
        }
        return checkIn != null && checkOut != null && checkOut.isAfter(checkIn);
    }

    @AssertTrue(message = "minPrice must be lower than or equal to maxPrice")
    public boolean isPriceRangeValid() {
        return minPrice == null || maxPrice == null || minPrice.compareTo(maxPrice) <= 0;
    }
}
