package com.travelmate.accommodation.service.impl;

import com.travelmate.accommodation.config.StayCostProperties;
import com.travelmate.accommodation.dto.StayCostRequest;
import com.travelmate.accommodation.dto.StayCostResponse;
import com.travelmate.accommodation.entity.Accommodation;
import com.travelmate.accommodation.entity.BookedPeriod;
import com.travelmate.accommodation.entity.PeriodStatus;
import com.travelmate.accommodation.exception.AvailabilityConflictException;
import com.travelmate.accommodation.exception.InvalidRequestException;
import com.travelmate.accommodation.exception.ResourceNotFoundException;
import com.travelmate.accommodation.repository.AccommodationRepository;
import com.travelmate.accommodation.repository.BookedPeriodRepository;
import com.travelmate.accommodation.service.StayCostService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * total = nights x pricePerNight            (subtotal)
 *       + cleaning fee                      (flat, depends on type)
 *       + subtotal x service fee rate       (service fee)
 *       + guests x nights x city tax        (city tax per person per night)
 */
@Service
@Transactional(readOnly = true)
public class StayCostServiceImpl implements StayCostService {

    private final AccommodationRepository accommodationRepository;
    private final BookedPeriodRepository periodRepository;
    private final StayCostProperties properties;
    private final Clock clock;

    public StayCostServiceImpl(AccommodationRepository accommodationRepository,
                               BookedPeriodRepository periodRepository,
                               StayCostProperties properties,
                               @Qualifier("accommodationClock") Clock clock) {
        this.accommodationRepository = accommodationRepository;
        this.periodRepository = periodRepository;
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    public StayCostResponse calculate(Long accommodationId, StayCostRequest request) {
        Accommodation acc = accommodationRepository.findById(accommodationId)
                .orElseThrow(() -> new ResourceNotFoundException("Accommodation", accommodationId));

        LocalDate checkIn = request.checkIn();
        LocalDate checkOut = request.checkOut();
        int guests = request.guests();
        validate(acc, checkIn, checkOut, guests);

        List<BookedPeriod> conflicts = periodRepository.findOverlapping(accommodationId, PeriodStatus.BOOKED, checkIn, checkOut);
        if (!conflicts.isEmpty()) {
            throw new AvailabilityConflictException("Not available: already booked from "
                    + conflicts.get(0).getStartDate() + " to " + conflicts.get(0).getEndDate());
        }

        long nights = ChronoUnit.DAYS.between(checkIn, checkOut);
        BigDecimal price = acc.getPricePerNight();
        BigDecimal subtotal = money(price.multiply(BigDecimal.valueOf(nights)));
        BigDecimal cleaningFee = money(properties.cleaningFeeFor(acc.getType()));
        BigDecimal serviceFee = money(subtotal.multiply(properties.getServiceFeeRate()));
        BigDecimal cityTaxRate = money(properties.cityTaxFor(acc.getCity()));
        BigDecimal cityTax = money(cityTaxRate.multiply(BigDecimal.valueOf(guests * nights)));
        BigDecimal total = subtotal.add(cleaningFee).add(serviceFee).add(cityTax);

        return new StayCostResponse(acc.getId(), acc.getName(), checkIn, checkOut, guests, nights,
                price, subtotal, cleaningFee, properties.getServiceFeeRate(), serviceFee,
                cityTaxRate, cityTax, total, properties.getCurrency());
    }

    private void validate(Accommodation acc, LocalDate checkIn, LocalDate checkOut, int guests) {
        if (!checkOut.isAfter(checkIn)) {
            throw new InvalidRequestException("checkOut must be after checkIn");
        }
        if (checkIn.isBefore(LocalDate.now(clock))) {
            throw new InvalidRequestException("checkIn cannot be in the past");
        }
        long nights = ChronoUnit.DAYS.between(checkIn, checkOut);
        if (nights > properties.getMaxNights()) {
            throw new InvalidRequestException("A stay cannot exceed " + properties.getMaxNights() + " nights");
        }
        if (guests > acc.getMaxGuests()) {
            throw new InvalidRequestException("This accommodation hosts at most " + acc.getMaxGuests()
                    + " guest(s), " + guests + " requested");
        }
    }

    private static BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
