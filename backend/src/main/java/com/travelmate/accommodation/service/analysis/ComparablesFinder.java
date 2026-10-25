package com.travelmate.accommodation.service.analysis;

import com.travelmate.accommodation.dto.AccommodationSummary;
import com.travelmate.accommodation.entity.Accommodation;
import com.travelmate.accommodation.exception.ResourceNotFoundException;
import com.travelmate.accommodation.mapper.AccommodationMapper;
import com.travelmate.accommodation.repository.AccommodationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

import static com.travelmate.accommodation.specification.AccommodationSpecifications.*;

/**
 * Database side of the price analysis. Returns plain DTO snapshots so the (slow) LLM call
 * happens outside of any transaction.
 */
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ComparablesFinder {

    public static final int MIN_COMPARABLES = 3;
    public static final int MAX_COMPARABLES = 10;

    private final AccommodationRepository repository;
    private final AccommodationMapper mapper;

    public AccommodationSummary loadTarget(Long id) {
        return repository.findById(id).map(mapper::toSummary)
                .orElseThrow(() -> new ResourceNotFoundException("Accommodation", id));
    }

    /**
     * Same city + same type + stars +/-1; if fewer than 3, same country + same type + stars +/-1;
     * if still fewer than 3, same country + any type + stars +/-1. Ranked by similarity, max 10.
     */
    public ComparablesResult findComparables(AccommodationSummary target) {
        Specification<Accommodation> base = Specification.allOf(
                idNot(target.id()),
                countryEquals(target.country()),
                starsBetween(target.stars() - 1, target.stars() + 1));

        List<AccommodationSummary> found = query(target, base.and(cityEquals(target.city())).and(hasType(target.type())));
        if (found.size() >= MIN_COMPARABLES) {
            return new ComparablesResult("CITY", found);
        }
        found = query(target, base.and(hasType(target.type())));
        if (found.size() >= MIN_COMPARABLES) {
            return new ComparablesResult("COUNTRY", found);
        }
        return new ComparablesResult("COUNTRY_ANY_TYPE", query(target, base));
    }

    /** Same-country listings cheaper than the target, fitting the guests and free on the given dates. */
    public List<AccommodationSummary> findCheaperCandidates(AccommodationSummary target, LocalDate checkIn,
                                                            LocalDate checkOut, Integer guests) {
        Specification<Accommodation> spec = Specification.allOf(
                idNot(target.id()),
                countryEquals(target.country()),
                priceLowerThan(target.pricePerNight()),
                fitsGuests(guests),
                availableBetween(checkIn, checkOut));
        return repository.findAll(spec).stream().map(mapper::toSummary).toList();
    }

    private List<AccommodationSummary> query(AccommodationSummary target, Specification<Accommodation> spec) {
        return repository.findAll(spec).stream()
                .map(mapper::toSummary)
                .sorted(Comparator.comparingDouble((AccommodationSummary c) -> SimilarityScorer.score(target, c)).reversed())
                .limit(MAX_COMPARABLES)
                .toList();
    }
}
