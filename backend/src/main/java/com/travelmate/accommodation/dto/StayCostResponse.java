package com.travelmate.accommodation.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record StayCostResponse(
        Long accommodationId,
        String accommodationName,
        LocalDate checkIn,
        LocalDate checkOut,
        int guests,
        long nights,
        BigDecimal pricePerNight,
        BigDecimal subtotal,
        BigDecimal cleaningFee,
        BigDecimal serviceFeeRate,
        BigDecimal serviceFee,
        BigDecimal cityTaxPerPersonPerNight,
        BigDecimal cityTax,
        BigDecimal total,
        String currency
) {
}
