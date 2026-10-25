package com.travelmate.accommodation.ai;

import com.travelmate.accommodation.dto.PriceVerdict;

import java.math.BigDecimal;
import java.util.List;

/** LLM estimate after validation: clamped price, recomputed difference and verdict. */
public record ParsedPriceEstimate(
        BigDecimal estimatedFairPrice,
        PriceVerdict priceVerdict,
        double differencePercent,
        String explanation,
        List<String> keyFactors,
        boolean clamped
) {
}
