package com.travelmate.accommodation.dto;

import java.time.LocalDate;
import java.util.List;

public record AvailabilityResponse(
        Long accommodationId,
        LocalDate checkIn,
        LocalDate checkOut,
        boolean available,
        List<BookedPeriodResponse> conflicts
) {
}
