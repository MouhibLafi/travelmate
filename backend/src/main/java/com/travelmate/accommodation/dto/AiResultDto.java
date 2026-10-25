package com.travelmate.accommodation.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Fair price estimate. source = AI (validated llama3 answer) or STATISTICAL (fallback on the baseline).
 * differencePercent = (currentPrice - estimatedFairPrice) / estimatedFairPrice * 100, always recomputed server side.
 */
public record AiResultDto(
        BigDecimal estimatedFairPrice,
        PriceVerdict priceVerdict,
        double differencePercent,
        String explanation,
        List<String> keyFactors,
        String source,
        boolean clamped,
        String model,
        Long durationMs
) {
}
