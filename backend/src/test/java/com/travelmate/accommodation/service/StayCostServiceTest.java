package com.travelmate.accommodation.service;

import com.travelmate.accommodation.config.StayCostProperties;
import com.travelmate.accommodation.dto.StayCostRequest;
import com.travelmate.accommodation.dto.StayCostResponse;
import com.travelmate.accommodation.entity.Accommodation;
import com.travelmate.accommodation.entity.AccommodationType;
import com.travelmate.accommodation.entity.BookedPeriod;
import com.travelmate.accommodation.entity.PeriodStatus;
import com.travelmate.accommodation.exception.AvailabilityConflictException;
import com.travelmate.accommodation.exception.InvalidRequestException;
import com.travelmate.accommodation.exception.ResourceNotFoundException;
import com.travelmate.accommodation.repository.AccommodationRepository;
import com.travelmate.accommodation.repository.BookedPeriodRepository;
import com.travelmate.accommodation.service.impl.StayCostServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.*;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StayCostServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 6, 1);

    @Mock
    private AccommodationRepository accommodationRepository;
    @Mock
    private BookedPeriodRepository periodRepository;

    private StayCostService service;
    private Accommodation hotel;

    @BeforeEach
    void setUp() {
        StayCostProperties props = new StayCostProperties();
        props.setServiceFeeRate(new BigDecimal("0.08"));
        props.setDefaultCleaningFee(new BigDecimal("15"));
        props.getCleaningFees().put(AccommodationType.APARTMENT, new BigDecimal("25"));
        props.setDefaultCityTax(new BigDecimal("3.00"));
        props.setCityTaxes(Map.of("rome", new BigDecimal("6.00")));
        props.setMaxNights(60);
        Clock clock = Clock.fixed(TODAY.atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC);
        service = new StayCostServiceImpl(accommodationRepository, periodRepository, props, clock);

        hotel = Accommodation.builder().id(1L).name("Hotel Colosseo").type(AccommodationType.HOTEL)
                .city("Rome").country("Italy").stars(4).rating(4.5)
                .pricePerNight(new BigDecimal("185.00")).maxGuests(3).build();
        lenient().when(accommodationRepository.findById(1L)).thenReturn(Optional.of(hotel));
        lenient().when(periodRepository.findOverlapping(eq(1L), eq(PeriodStatus.BOOKED), any(), any())).thenReturn(List.of());
    }

    @Test
    void computesFullBreakdown() {
        StayCostResponse r = service.calculate(1L, new StayCostRequest(TODAY.plusDays(10), TODAY.plusDays(13), 2));

        assertThat(r.nights()).isEqualTo(3);
        assertThat(r.subtotal()).isEqualByComparingTo("555.00");          // 3 x 185
        assertThat(r.cleaningFee()).isEqualByComparingTo("15.00");        // hotel default
        assertThat(r.serviceFee()).isEqualByComparingTo("44.40");         // 8% of 555
        assertThat(r.cityTaxPerPersonPerNight()).isEqualByComparingTo("6.00");
        assertThat(r.cityTax()).isEqualByComparingTo("36.00");            // 2 guests x 3 nights x 6
        assertThat(r.total()).isEqualByComparingTo("650.40");
        assertThat(r.currency()).isEqualTo("EUR");
    }

    @Test
    void usesTypeCleaningFeeAndDefaultCityTax() {
        hotel.setType(AccommodationType.APARTMENT);
        hotel.setCity("Florence");
        hotel.setPricePerNight(new BigDecimal("100.00"));

        StayCostResponse r = service.calculate(1L, new StayCostRequest(TODAY, TODAY.plusDays(2), 1));

        assertThat(r.cleaningFee()).isEqualByComparingTo("25.00");
        assertThat(r.cityTax()).isEqualByComparingTo("6.00");             // 1 x 2 x 3.00 default
        assertThat(r.total()).isEqualByComparingTo("247.00");             // 200 + 25 + 16 + 6
    }

    @Test
    void rejectsCheckOutBeforeCheckIn() {
        assertThatThrownBy(() -> service.calculate(1L, new StayCostRequest(TODAY.plusDays(5), TODAY.plusDays(5), 1)))
                .isInstanceOf(InvalidRequestException.class).hasMessageContaining("after checkIn");
    }

    @Test
    void rejectsPastCheckIn() {
        assertThatThrownBy(() -> service.calculate(1L, new StayCostRequest(TODAY.minusDays(1), TODAY.plusDays(2), 1)))
                .isInstanceOf(InvalidRequestException.class).hasMessageContaining("past");
    }

    @Test
    void rejectsTooManyGuests() {
        assertThatThrownBy(() -> service.calculate(1L, new StayCostRequest(TODAY.plusDays(1), TODAY.plusDays(2), 4)))
                .isInstanceOf(InvalidRequestException.class).hasMessageContaining("at most 3");
    }

    @Test
    void rejectsTooLongStay() {
        assertThatThrownBy(() -> service.calculate(1L, new StayCostRequest(TODAY.plusDays(1), TODAY.plusDays(70), 1)))
                .isInstanceOf(InvalidRequestException.class).hasMessageContaining("60 nights");
    }

    @Test
    void rejectsBookedDates() {
        BookedPeriod booked = BookedPeriod.builder().startDate(TODAY.plusDays(3)).endDate(TODAY.plusDays(6))
                .status(PeriodStatus.BOOKED).accommodation(hotel).build();
        when(periodRepository.findOverlapping(eq(1L), eq(PeriodStatus.BOOKED), any(), any())).thenReturn(List.of(booked));

        assertThatThrownBy(() -> service.calculate(1L, new StayCostRequest(TODAY.plusDays(4), TODAY.plusDays(8), 1)))
                .isInstanceOf(AvailabilityConflictException.class);
    }

    @Test
    void unknownAccommodationIs404() {
        when(accommodationRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.calculate(99L, new StayCostRequest(TODAY.plusDays(1), TODAY.plusDays(2), 1)))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
