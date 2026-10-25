package com.travelmate.accommodation.service.analysis;

import com.travelmate.accommodation.dto.AccommodationSummary;

import java.util.List;

public record ComparablesResult(String scope, List<AccommodationSummary> comparables) {
}
