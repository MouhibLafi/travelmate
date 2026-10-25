package com.travelmate.accommodation.service.impl;

import com.travelmate.accommodation.config.CacheConfig;
import com.travelmate.accommodation.dto.AvailabilityResponse;
import com.travelmate.accommodation.dto.BookedDatesResponse;
import com.travelmate.accommodation.dto.BookedPeriodRequest;
import com.travelmate.accommodation.dto.BookedPeriodResponse;
import com.travelmate.accommodation.entity.Accommodation;
import com.travelmate.accommodation.entity.BookedPeriod;
import com.travelmate.accommodation.entity.PeriodStatus;
import com.travelmate.accommodation.exception.AvailabilityConflictException;
import com.travelmate.accommodation.exception.InvalidRequestException;
import com.travelmate.accommodation.exception.ResourceNotFoundException;
import com.travelmate.accommodation.mapper.AccommodationMapper;
import com.travelmate.accommodation.repository.AccommodationRepository;
import com.travelmate.accommodation.repository.BookedPeriodRepository;
import com.travelmate.accommodation.service.AvailabilityService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.TreeSet;

@Service
@Transactional(readOnly = true)
public class AvailabilityServiceImpl implements AvailabilityService {

    private static final int MAX_CALENDAR_DAYS = 400;

    private final AccommodationRepository accommodationRepository;
    private final BookedPeriodRepository periodRepository;
    private final AccommodationMapper mapper;
    private final Clock clock;

    public AvailabilityServiceImpl(AccommodationRepository accommodationRepository,
                                   BookedPeriodRepository periodRepository,
                                   AccommodationMapper mapper,
                                   @Qualifier("accommodationClock") Clock clock) {
        this.accommodationRepository = accommodationRepository;
        this.periodRepository = periodRepository;
        this.mapper = mapper;
        this.clock = clock;
    }

    @Override
    public AvailabilityResponse checkAvailability(Long accommodationId, LocalDate checkIn, LocalDate checkOut) {
        ensureExists(accommodationId);
        if (!checkOut.isAfter(checkIn)) {
            throw new InvalidRequestException("checkOut must be after checkIn");
        }
        List<BookedPeriodResponse> conflicts = periodRepository
                .findOverlapping(accommodationId, PeriodStatus.BOOKED, checkIn, checkOut)
                .stream().map(mapper::toPeriodResponse).toList();
        return new AvailabilityResponse(accommodationId, checkIn, checkOut, conflicts.isEmpty(), conflicts);
    }

    @Override
    public BookedDatesResponse getBookedDates(Long accommodationId, LocalDate from, LocalDate to) {
        ensureExists(accommodationId);
        LocalDate start = from != null ? from : LocalDate.now(clock);
        LocalDate end = to != null ? to : start.plusMonths(6);
        if (!end.isAfter(start)) {
            throw new InvalidRequestException("'to' must be after 'from'");
        }
        if (ChronoUnit.DAYS.between(start, end) > MAX_CALENDAR_DAYS) {
            throw new InvalidRequestException("Date window cannot exceed " + MAX_CALENDAR_DAYS + " days");
        }
        List<BookedPeriod> periods = periodRepository.findOverlapping(accommodationId, PeriodStatus.BOOKED, start, end);

        // Every booked night inside the window (end dates are exclusive = check-out day stays free).
        TreeSet<LocalDate> nights = new TreeSet<>();
        for (BookedPeriod p : periods) {
            LocalDate d = p.getStartDate().isBefore(start) ? start : p.getStartDate();
            LocalDate last = p.getEndDate().isAfter(end) ? end : p.getEndDate();
            for (; d.isBefore(last); d = d.plusDays(1)) {
                nights.add(d);
            }
        }
        return new BookedDatesResponse(accommodationId, start, end,
                periods.stream().map(mapper::toPeriodResponse).toList(), List.copyOf(nights));
    }

    @Override
    public List<BookedPeriodResponse> listPeriods(Long accommodationId) {
        ensureExists(accommodationId);
        return periodRepository.findByAccommodationIdOrderByStartDateAsc(accommodationId)
                .stream().map(mapper::toPeriodResponse).toList();
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = CacheConfig.PRICE_ANALYSIS_CACHE, allEntries = true)
    public BookedPeriodResponse addPeriod(Long accommodationId, BookedPeriodRequest request) {
        Accommodation accommodation = accommodationRepository.findById(accommodationId)
                .orElseThrow(() -> new ResourceNotFoundException("Accommodation", accommodationId));
        PeriodStatus status = request.status() != null ? request.status() : PeriodStatus.BOOKED;

        if (status == PeriodStatus.BOOKED) {
            List<BookedPeriod> overlapping = periodRepository.findOverlapping(
                    accommodationId, PeriodStatus.BOOKED, request.startDate(), request.endDate());
            if (!overlapping.isEmpty()) {
                BookedPeriod first = overlapping.get(0);
                throw new AvailabilityConflictException("Dates overlap an existing booking from "
                        + first.getStartDate() + " to " + first.getEndDate());
            }
        }
        BookedPeriod period = BookedPeriod.builder()
                .startDate(request.startDate())
                .endDate(request.endDate())
                .status(status)
                .note(request.note())
                .build();
        accommodation.addPeriod(period);
        return mapper.toPeriodResponse(periodRepository.save(period));
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = CacheConfig.PRICE_ANALYSIS_CACHE, allEntries = true)
    public void deletePeriod(Long accommodationId, Long periodId) {
        BookedPeriod period = periodRepository.findByIdAndAccommodationId(periodId, accommodationId)
                .orElseThrow(() -> new ResourceNotFoundException("Period", periodId));
        period.getAccommodation().getPeriods().remove(period);
        periodRepository.delete(period);
    }

    private void ensureExists(Long accommodationId) {
        if (!accommodationRepository.existsById(accommodationId)) {
            throw new ResourceNotFoundException("Accommodation", accommodationId);
        }
    }
}
