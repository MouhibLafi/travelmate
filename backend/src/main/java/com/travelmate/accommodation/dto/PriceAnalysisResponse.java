package com.travelmate.accommodation.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record PriceAnalysisResponse(
        AccommodationSummary target,
        BaselineDto baseline,
        AiResultDto aiResult,
        List<AlternativeDto> alternatives,
        boolean aiAvailable,
        String aiMessage,
        LocalDate checkIn,
        LocalDate checkOut,
        Instant generatedAt
) {
}
