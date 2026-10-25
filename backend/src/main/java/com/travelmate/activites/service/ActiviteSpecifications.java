package com.travelmate.activites.service;

import com.travelmate.activites.model.Activite;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** Construit dynamiquement la requête de recherche/filtres des activités. */
public final class ActiviteSpecifications {

    private ActiviteSpecifications() {
    }

    public static Specification<Activite> filtrer(Long destinationId, String categorie,
                                                  BigDecimal prixMin, BigDecimal prixMax,
                                                  Integer dureeMax, String q) {
        return (root, query, cb) -> {
            List<Predicate> predicats = new ArrayList<>();

            if (destinationId != null) {
                predicats.add(cb.equal(root.get("destination").get("id"), destinationId));
            }
            if (categorie != null && !categorie.isBlank()) {
                predicats.add(cb.equal(cb.lower(root.get("categorie").get("nom")), categorie.toLowerCase()));
            }
            if (prixMin != null) {
                predicats.add(cb.greaterThanOrEqualTo(root.get("prix"), prixMin));
            }
            if (prixMax != null) {
                predicats.add(cb.lessThanOrEqualTo(root.get("prix"), prixMax));
            }
            if (dureeMax != null) {
                predicats.add(cb.lessThanOrEqualTo(root.get("dureeMinutes"), dureeMax));
            }
            if (q != null && !q.isBlank()) {
                String motif = "%" + q.toLowerCase() + "%";
                predicats.add(cb.or(
                        cb.like(cb.lower(root.get("nom")), motif),
                        cb.like(cb.lower(root.get("description")), motif),
                        cb.like(cb.lower(root.get("tags")), motif)
                ));
            }
            return cb.and(predicats.toArray(new Predicate[0]));
        };
    }
}
