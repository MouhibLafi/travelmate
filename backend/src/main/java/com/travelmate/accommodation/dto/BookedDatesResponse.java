package com.travelmate.accommodation.dto;

import java.time.LocalDate;
import java.util.List;

/** Booked nights between {@code from} and {@code to}, ready to disable days in a calendar. */
public record BookedDatesResponse(
        Long accommodationId,
        LocalDate from,
        LocalDate to,
        List<BookedPeriodResponse> periods,
        List<LocalDate> bookedDates
) {
}
