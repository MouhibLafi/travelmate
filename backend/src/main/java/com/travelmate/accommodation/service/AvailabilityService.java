package com.travelmate.accommodation.service;

import com.travelmate.accommodation.dto.AvailabilityResponse;
import com.travelmate.accommodation.dto.BookedDatesResponse;
import com.travelmate.accommodation.dto.BookedPeriodRequest;
import com.travelmate.accommodation.dto.BookedPeriodResponse;

import java.time.LocalDate;
import java.util.List;

public interface AvailabilityService {

    AvailabilityResponse checkAvailability(Long accommodationId, LocalDate checkIn, LocalDate checkOut);

    BookedDatesResponse getBookedDates(Long accommodationId, LocalDate from, LocalDate to);

    List<BookedPeriodResponse> listPeriods(Long accommodationId);

    BookedPeriodResponse addPeriod(Long accommodationId, BookedPeriodRequest request);

    void deletePeriod(Long accommodationId, Long periodId);
}
