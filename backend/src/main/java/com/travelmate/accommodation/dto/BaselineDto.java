package com.travelmate.accommodation.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Statistical part of the analysis.
 * scope: CITY (same city/type, stars +/-1), COUNTRY (same country/type) or COUNTRY_ANY_TYPE.
 */
public record BaselineDto(
        BigDecimal baselinePrice,
        BigDecimal medianComparablePrice,
        BigDecimal minComparablePrice,
        BigDecimal maxComparablePrice,
        int comparablesCount,
        String scope,
        List<ComparableDto> comparables
) {
}
