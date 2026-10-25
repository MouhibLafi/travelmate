package com.travelmate.accommodation.specification;

import com.travelmate.accommodation.dto.AccommodationSearchCriteria;
import com.travelmate.accommodation.entity.Accommodation;
import com.travelmate.accommodation.entity.AccommodationType;
import com.travelmate.accommodation.entity.Amenity;
import com.travelmate.accommodation.entity.BookedPeriod;
import com.travelmate.accommodation.entity.PeriodStatus;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Locale;

/** Composable JPA Specifications for the accommodation search. Each method returns null when the filter is unused. */
public final class AccommodationSpecifications {

    private AccommodationSpecifications() {
    }

    public static Specification<Accommodation> fromCriteria(AccommodationSearchCriteria c) {
        return Specification.allOf(
                textContains(c.getQ()),
                cityContains(c.getCity()),
                hasType(c.getType()),
                priceBetween(c.getMinPrice(), c.getMaxPrice()),
                minStars(c.getMinStars()),
                minRating(c.getMinRating()),
                hasAllAmenities(c.getAmenities()),
                fitsGuests(c.getGuests()),
                availableBetween(c.getCheckIn(), c.getCheckOut()));
    }

    public static Specification<Accommodation> textContains(String q) {
        if (!StringUtils.hasText(q)) {
            return null;
        }
        String pattern = like(q);
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("name")), pattern),
                cb.like(cb.lower(root.get("city")), pattern),
                cb.like(cb.lower(root.get("country")), pattern));
    }

    public static Specification<Accommodation> cityContains(String city) {
        if (!StringUtils.hasText(city)) {
            return null;
        }
        return (root, query, cb) -> cb.like(cb.lower(root.get("city")), like(city));
    }

    public static Specification<Accommodation> cityEquals(String city) {
        return city == null ? null : (root, query, cb) -> cb.equal(cb.lower(root.get("city")), city.toLowerCase(Locale.ROOT));
    }

    public static Specification<Accommodation> countryEquals(String country) {
        return country == null ? null : (root, query, cb) -> cb.equal(cb.lower(root.get("country")), country.toLowerCase(Locale.ROOT));
    }

    public static Specification<Accommodation> hasType(AccommodationType type) {
        return type == null ? null : (root, query, cb) -> cb.equal(root.get("type"), type);
    }

    public static Specification<Accommodation> priceBetween(BigDecimal min, BigDecimal max) {
        if (min == null && max == null) {
            return null;
        }
        return (root, query, cb) -> {
            if (min != null && max != null) {
                return cb.between(root.get("pricePerNight"), min, max);
            }
            return min != null
                    ? cb.greaterThanOrEqualTo(root.get("pricePerNight"), min)
                    : cb.lessThanOrEqualTo(root.get("pricePerNight"), max);
        };
    }

    public static Specification<Accommodation> priceLowerThan(BigDecimal price) {
        return (root, query, cb) -> cb.lessThan(root.get("pricePerNight"), price);
    }

    public static Specification<Accommodation> minStars(Integer stars) {
        return stars == null ? null : (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("stars"), stars);
    }

    public static Specification<Accommodation> starsBetween(int min, int max) {
        return (root, query, cb) -> cb.between(root.get("stars"), min, max);
    }

    public static Specification<Accommodation> minRating(Double rating) {
        return rating == null ? null : (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("rating"), rating);
    }

    public static Specification<Accommodation> fitsGuests(Integer guests) {
        return guests == null ? null : (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("maxGuests"), guests);
    }

    public static Specification<Accommodation> idNot(Long id) {
        return (root, query, cb) -> cb.notEqual(root.get("id"), id);
    }

    /** One "member of" predicate per amenity, so the accommodation must offer all of them. */
    public static Specification<Accommodation> hasAllAmenities(Collection<Amenity> amenities) {
        if (amenities == null || amenities.isEmpty()) {
            return null;
        }
        return (root, query, cb) -> cb.and(amenities.stream()
                .map(a -> cb.isMember(a, root.<Collection<Amenity>>get("amenities")))
                .toArray(jakarta.persistence.criteria.Predicate[]::new));
    }

    /** Excludes accommodations having a BOOKED period overlapping [checkIn, checkOut). */
    public static Specification<Accommodation> availableBetween(LocalDate checkIn, LocalDate checkOut) {
        if (checkIn == null || checkOut == null) {
            return null;
        }
        return (root, query, cb) -> {
            Subquery<Long> booked = query.subquery(Long.class);
            Root<BookedPeriod> p = booked.from(BookedPeriod.class);
            booked.select(p.get("id")).where(
                    cb.equal(p.get("accommodation"), root),
                    cb.equal(p.get("status"), PeriodStatus.BOOKED),
                    cb.lessThan(p.get("startDate"), checkOut),
                    cb.greaterThan(p.get("endDate"), checkIn));
            return cb.not(cb.exists(booked));
        };
    }

    private static String like(String value) {
        String escaped = value.trim().toLowerCase(Locale.ROOT).replace("%", "").replace("_", "");
        return "%" + escaped + "%";
    }
}
