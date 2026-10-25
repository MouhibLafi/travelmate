package com.travelmate.accommodation.service.analysis;

import com.travelmate.accommodation.dto.AccommodationSummary;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;

/**
 * Statistical fair-price baseline.
 * <p>
 * Each comparable price is first normalised to the target's characteristics:
 * <pre>
 * normalised = price x (1 + 0.12 x deltaStars) x (1 + 0.06 x deltaRating) x (1 + 0.02 x deltaAmenities)
 * </pre>
 * where delta = target - comparable (the combined multiplier is kept within [0.6, 1.6]).
 * The baseline is the median of the normalised prices; the median is robust to luxury outliers.
 */
@Component
public class BaselineCalculator {

    static final double STAR_ADJUSTMENT = 0.12;
    static final double RATING_ADJUSTMENT = 0.06;
    static final double AMENITY_ADJUSTMENT = 0.02;
    static final double MIN_MULTIPLIER = 0.6;
    static final double MAX_MULTIPLIER = 1.6;

    public BaselineResult compute(AccommodationSummary target, List<AccommodationSummary> comparables) {
        if (comparables == null || comparables.isEmpty()) {
            return new BaselineResult(money(target.pricePerNight()), null, null, null, 0);
        }
        List<BigDecimal> rawPrices = comparables.stream().map(AccommodationSummary::pricePerNight).toList();
        List<BigDecimal> normalised = comparables.stream().map(c -> normalise(target, c)).toList();

        return new BaselineResult(
                money(median(normalised)),
                money(median(rawPrices)),
                money(rawPrices.stream().min(Comparator.naturalOrder()).orElseThrow()),
                money(rawPrices.stream().max(Comparator.naturalOrder()).orElseThrow()),
                comparables.size());
    }

    /** Price the comparable would have if it had the target's stars, rating and amenity count. */
    BigDecimal normalise(AccommodationSummary target, AccommodationSummary comparable) {
        double multiplier = (1 + STAR_ADJUSTMENT * (target.stars() - comparable.stars()))
                * (1 + RATING_ADJUSTMENT * (target.rating() - comparable.rating()))
                * (1 + AMENITY_ADJUSTMENT * (amenityCount(target) - amenityCount(comparable)));
        multiplier = Math.max(MIN_MULTIPLIER, Math.min(MAX_MULTIPLIER, multiplier));
        return comparable.pricePerNight().multiply(BigDecimal.valueOf(multiplier));
    }

    static BigDecimal median(List<BigDecimal> values) {
        List<BigDecimal> sorted = values.stream().sorted().toList();
        int n = sorted.size();
        if (n % 2 == 1) {
            return sorted.get(n / 2);
        }
        return sorted.get(n / 2 - 1).add(sorted.get(n / 2)).divide(BigDecimal.valueOf(2), 4, RoundingMode.HALF_UP);
    }

    private static int amenityCount(AccommodationSummary a) {
        return a.amenities() == null ? 0 : a.amenities().size();
    }

    private static BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
