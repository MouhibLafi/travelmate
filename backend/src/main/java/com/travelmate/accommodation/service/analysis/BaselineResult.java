package com.travelmate.accommodation.service.analysis;

import java.math.BigDecimal;

/** Output of {@link BaselineCalculator}. Comparable statistics are null when there is no comparable. */
public record BaselineResult(
        BigDecimal baselinePrice,
        BigDecimal medianComparablePrice,
        BigDecimal minComparablePrice,
        BigDecimal maxComparablePrice,
        int comparablesCount
) {
}
