package com.travelmate.accommodation.dto;

import com.travelmate.accommodation.entity.PeriodStatus;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record BookedPeriodRequest(
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate,
        PeriodStatus status,
        @Size(max = 255) String note
) {
    @AssertTrue(message = "endDate must be after startDate")
    public boolean isRangeValid() {
        return startDate == null || endDate == null || endDate.isAfter(startDate);
    }
}
