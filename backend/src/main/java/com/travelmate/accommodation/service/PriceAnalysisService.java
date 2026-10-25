package com.travelmate.accommodation.service;

import com.travelmate.accommodation.dto.AiStatusResponse;
import com.travelmate.accommodation.dto.PriceAnalysisResponse;

import java.time.LocalDate;

public interface PriceAnalysisService {

    /** Fair price estimate + cheaper alternatives. Never fails because of the AI: falls back to statistics. */
    PriceAnalysisResponse analyze(Long accommodationId, LocalDate checkIn, LocalDate checkOut, Integer guests);

    AiStatusResponse aiStatus();
}
