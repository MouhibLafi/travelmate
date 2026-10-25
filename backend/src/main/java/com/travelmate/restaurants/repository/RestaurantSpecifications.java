package com.travelmate.restaurants.repository;

import com.travelmate.restaurants.entity.Restaurant;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/** Construit dynamiquement la requête de recherche selon les filtres reçus du frontend. */
public final class RestaurantSpecifications {

    private RestaurantSpecifications() {
    }

    public static Specification<Restaurant> avecFiltres(String recherche, String cuisine, String ville,
                                                        Double prixMin, Double prixMax, Double noteMin,
                                                        List<String> options) {
        return (root, query, cb) -> {
            List<Predicate> predicats = new ArrayList<>();

            if (recherche != null && !recherche.isBlank()) {
                String motif = "%" + recherche.trim().toLowerCase() + "%";
                predicats.add(cb.or(
                        cb.like(cb.lower(root.get("nom")), motif),
                        cb.like(cb.lower(root.get("cuisine")), motif),
                        cb.like(cb.lower(root.get("description")), motif),
                        cb.like(cb.lower(root.get("ville")), motif)
                ));
            }
            if (cuisine != null && !cuisine.isBlank()) {
                predicats.add(cb.equal(cb.lower(root.get("cuisine")), cuisine.trim().toLowerCase()));
            }
            if (ville != null && !ville.isBlank()) {
                predicats.add(cb.equal(cb.lower(root.get("ville")), ville.trim().toLowerCase()));
            }
            if (prixMin != null) {
                predicats.add(cb.greaterThanOrEqualTo(root.get("prixMoyen"), prixMin));
            }
            if (prixMax != null) {
                predicats.add(cb.lessThanOrEqualTo(root.get("prixMoyen"), prixMax));
            }
            if (noteMin != null) {
                predicats.add(cb.greaterThanOrEqualTo(root.get("noteMoyenne"), noteMin));
            }
            if (options != null) {
                for (String option : options) {
                    if (option == null || option.isBlank()) {
                        continue;
                    }
                    query.distinct(true);
                    Join<Restaurant, String> jointure = root.joinSet("optionsAlimentaires");
                    predicats.add(cb.equal(cb.lower(jointure.as(String.class)), option.trim().toLowerCase()));
                }
            }
            return cb.and(predicats.toArray(new Predicate[0]));
        };
    }
}
