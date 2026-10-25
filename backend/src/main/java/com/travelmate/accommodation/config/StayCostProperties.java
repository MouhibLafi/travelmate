package com.travelmate.accommodation.config;

import com.travelmate.accommodation.entity.AccommodationType;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/** Fees used by the stay cost calculator (travelmate.accommodation.pricing.*). */
@Getter
@Setter
@ConfigurationProperties(prefix = "travelmate.accommodation.pricing")
public class StayCostProperties {

    private String currency = "EUR";
    private BigDecimal serviceFeeRate = new BigDecimal("0.08");
    private BigDecimal defaultCleaningFee = new BigDecimal("15");
    private Map<AccommodationType, BigDecimal> cleaningFees = new EnumMap<>(AccommodationType.class);
    private BigDecimal defaultCityTax = new BigDecimal("3.00");
    /** City tax per person per night, keyed by lower-case city name. */
    private Map<String, BigDecimal> cityTaxes = new HashMap<>();
    private int maxNights = 60;

    public BigDecimal cleaningFeeFor(AccommodationType type) {
        return cleaningFees.getOrDefault(type, defaultCleaningFee);
    }

    public BigDecimal cityTaxFor(String city) {
        return city == null ? defaultCityTax : cityTaxes.getOrDefault(city.trim().toLowerCase(Locale.ROOT), defaultCityTax);
    }
}
