package com.travelmate.accommodation.dto;

public enum PriceVerdict {
    UNDERPRICED,
    FAIR,
    OVERPRICED;

    /** Prices within +/-10% of the fair price are considered FAIR. */
    public static final double FAIR_TOLERANCE_PERCENT = 10.0;

    public static PriceVerdict fromDifference(double differencePercent) {
        if (differencePercent > FAIR_TOLERANCE_PERCENT) {
            return OVERPRICED;
        }
        if (differencePercent < -FAIR_TOLERANCE_PERCENT) {
            return UNDERPRICED;
        }
        return FAIR;
    }
}
