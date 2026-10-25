package com.travelmate.itinerary.dto;

import com.travelmate.itinerary.entity.Itinerary;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record ItineraryDto(Long id,
                           Long tripId,
                           String destination,
                           LocalDate startDate,
                           LocalDate endDate,
                           double budget,
                           double totalEstimatedCost,
                           boolean overBudget,
                           boolean generatedByAi,
                           LocalDateTime createdAt,
                           List<DayDto> days) {

    public static ItineraryDto from(Itinerary it) {
        return new ItineraryDto(it.getId(), it.getTripId(), it.getDestination(), it.getStartDate(), it.getEndDate(),
                it.getBudget(), it.getTotalEstimatedCost(), it.isOverBudget(), it.isGeneratedByAi(), it.getCreatedAt(),
                it.getDays().stream().map(DayDto::from).toList());
    }
}
