package com.travelmate.accommodation.service.analysis;

import com.travelmate.accommodation.dto.AccommodationSummary;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static com.travelmate.accommodation.entity.AccommodationType.HOTEL;
import static com.travelmate.accommodation.entity.Amenity.*;
import static com.travelmate.accommodation.support.TestData.summary;
import static org.assertj.core.api.Assertions.assertThat;

class BaselineCalculatorTest {

    private final BaselineCalculator calculator = new BaselineCalculator();

    @Test
    void identicalProfilesGiveThePlainMedian() {
        AccommodationSummary target = summary(1, "Rome", HOTEL, 4, 4.5, 200, WIFI, BREAKFAST);
        List<AccommodationSummary> comps = List.of(
                summary(2, "Rome", HOTEL, 4, 4.5, 100, WIFI, POOL),
                summary(3, "Rome", HOTEL, 4, 4.5, 150, WIFI, GYM),
                summary(4, "Rome", HOTEL, 4, 4.5, 400, WIFI, SPA));   // outlier does not move the median

        BaselineResult r = calculator.compute(target, comps);

        assertThat(r.baselinePrice()).isEqualByComparingTo("150.00");
        assertThat(r.medianComparablePrice()).isEqualByComparingTo("150.00");
        assertThat(r.minComparablePrice()).isEqualByComparingTo("100.00");
        assertThat(r.maxComparablePrice()).isEqualByComparingTo("400.00");
        assertThat(r.comparablesCount()).isEqualTo(3);
    }

    @Test
    void evenCountUsesAverageOfTwoMiddleValues() {
        AccommodationSummary target = summary(1, "Rome", HOTEL, 3, 4.0, 100);
        List<AccommodationSummary> comps = List.of(
                summary(2, "Rome", HOTEL, 3, 4.0, 80),
                summary(3, "Rome", HOTEL, 3, 4.0, 90),
                summary(4, "Rome", HOTEL, 3, 4.0, 110),
                summary(5, "Rome", HOTEL, 3, 4.0, 200));

        assertThat(calculator.compute(target, comps).baselinePrice()).isEqualByComparingTo("100.00");
    }

    @Test
    void adjustsForStarsRatingAndAmenities() {
        // target has 1 more star, +0.5 rating and 1 more amenity than the comparable
        AccommodationSummary target = summary(1, "Rome", HOTEL, 4, 4.5, 200, WIFI, BREAKFAST);
        AccommodationSummary comp = summary(2, "Rome", HOTEL, 3, 4.0, 100, WIFI);

        BigDecimal normalised = calculator.normalise(target, comp);

        // 100 x 1.12 x 1.03 x 1.02 = 117.6672
        assertThat(normalised.doubleValue()).isCloseTo(117.67, org.assertj.core.data.Offset.offset(0.01));
    }

    @Test
    void adjustmentIsBounded() {
        AccommodationSummary target = summary(1, "Rome", HOTEL, 5, 5.0, 500, WIFI, POOL, SPA, GYM, BREAKFAST, PARKING);
        AccommodationSummary comp = summary(2, "Rome", HOTEL, 1, 1.0, 100);

        assertThat(calculator.normalise(target, comp).doubleValue()).isEqualTo(160.0); // capped at x1.6
    }

    @Test
    void noComparablesFallsBackToTargetPrice() {
        AccommodationSummary target = summary(1, "Rome", HOTEL, 4, 4.5, 185);

        BaselineResult r = calculator.compute(target, List.of());

        assertThat(r.baselinePrice()).isEqualByComparingTo("185.00");
        assertThat(r.comparablesCount()).isZero();
        assertThat(r.medianComparablePrice()).isNull();
    }
}
