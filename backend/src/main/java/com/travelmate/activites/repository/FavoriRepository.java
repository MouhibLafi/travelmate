package com.travelmate.activites.repository;

import com.travelmate.activites.model.Favori;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FavoriRepository extends JpaRepository<Favori, Long> {
    List<Favori> findByUserIdOrderByDateAjoutDesc(Long userId);

    Optional<Favori> findByUserIdAndActiviteId(Long userId, Long activiteId);

    boolean existsByUserIdAndActiviteId(Long userId, Long activiteId);

    long countByActiviteId(Long activiteId);
}
