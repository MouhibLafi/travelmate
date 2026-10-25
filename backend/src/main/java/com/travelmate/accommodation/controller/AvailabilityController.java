package com.travelmate.accommodation.controller;

import com.travelmate.accommodation.dto.*;
import com.travelmate.accommodation.service.AvailabilityService;
import com.travelmate.accommodation.service.StayCostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/accommodations/{id}")
@RequiredArgsConstructor
@Tag(name = "Availability & stay cost", description = "Module 5 - booked periods, calendar and stay cost calculator")
public class AvailabilityController {

    private final AvailabilityService availabilityService;
    private final StayCostService stayCostService;

    @GetMapping("/availability")
    @Operation(summary = "Is the accommodation free between checkIn and checkOut?")
    public AvailabilityResponse availability(@PathVariable Long id,
                                             @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
                                             @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut) {
        return availabilityService.checkAvailability(id, checkIn, checkOut);
    }

    @GetMapping("/booked-dates")
    @Operation(summary = "Booked nights for a calendar (defaults: today -> +6 months)")
    public BookedDatesResponse bookedDates(@PathVariable Long id,
                                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return availabilityService.getBookedDates(id, from, to);
    }

    @GetMapping("/periods")
    @Operation(summary = "All periods of an accommodation (admin)")
    public List<BookedPeriodResponse> periods(@PathVariable Long id) {
        return availabilityService.listPeriods(id);
    }

    @PostMapping("/periods")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add a period (409 if a BOOKED period overlaps)")
    public BookedPeriodResponse addPeriod(@PathVariable Long id, @Valid @RequestBody BookedPeriodRequest request) {
        return availabilityService.addPeriod(id, request);
    }

    @DeleteMapping("/periods/{periodId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove a period")
    public void deletePeriod(@PathVariable Long id, @PathVariable Long periodId) {
        availabilityService.deletePeriod(id, periodId);
    }

    @PostMapping("/stay-cost")
    @Operation(summary = "Compute the full cost of a stay (nights, fees, city tax, total)")
    public StayCostResponse stayCost(@PathVariable Long id, @Valid @RequestBody StayCostRequest request) {
        return stayCostService.calculate(id, request);
    }
}
