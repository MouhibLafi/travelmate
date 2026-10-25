package com.travelmate.accommodation.service.analysis;

import com.travelmate.accommodation.dto.AccommodationSummary;
import com.travelmate.accommodation.entity.Amenity;

import java.util.EnumSet;
import java.util.Set;

/**
 * Similarity between two listings, from 0 (unrelated) to 1 (identical profile):
 * 30% same city, 20% same type, 20% star proximity, 20% amenity overlap (Jaccard), 10% rating proximity.
 */
public final class SimilarityScorer {

    private SimilarityScorer() {
    }

    public static double score(AccommodationSummary target, AccommodationSummary other) {
        double city = target.city().equalsIgnoreCase(other.city()) ? 1 : 0;
        double type = target.type() == other.type() ? 1 : 0;
        double stars = 1 - Math.min(Math.abs(target.stars() - other.stars()), 4) / 4.0;
        double rating = 1 - Math.min(Math.abs(target.rating() - other.rating()), 5) / 5.0;
        double amenities = jaccard(target.amenities(), other.amenities());
        double score = 0.30 * city + 0.20 * type + 0.20 * stars + 0.20 * amenities + 0.10 * rating;
        return Math.round(score * 1000) / 1000.0;
    }

    public static double jaccard(Set<Amenity> a, Set<Amenity> b) {
        Set<Amenity> left = a == null || a.isEmpty() ? EnumSet.noneOf(Amenity.class) : EnumSet.copyOf(a);
        Set<Amenity> right = b == null || b.isEmpty() ? EnumSet.noneOf(Amenity.class) : EnumSet.copyOf(b);
        if (left.isEmpty() && right.isEmpty()) {
            return 1;
        }
        Set<Amenity> union = EnumSet.copyOf(left);
        union.addAll(right);
        left.retainAll(right);
        return (double) left.size() / union.size();
    }
}
