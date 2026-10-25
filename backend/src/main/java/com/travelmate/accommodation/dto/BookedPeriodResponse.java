package com.travelmate.accommodation.dto;

import com.travelmate.accommodation.entity.PeriodStatus;

import java.time.LocalDate;

public record BookedPeriodResponse(
        Long id,
        Long accommodationId,
        LocalDate startDate,
        LocalDate endDate,
        PeriodStatus status,
        String note
) {
}
