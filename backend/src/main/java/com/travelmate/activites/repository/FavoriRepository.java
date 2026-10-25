package com.travelmate.activites.repository;

import com.travelmate.activites.model.Favori;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository("activiteFavoriRepository")   // integration : nom distinct du FavoriRepository du module 4
public interface FavoriRepository extends JpaRepository<Favori, Long> {

    // Integration : quand un utilisateur est supprime (module 1), on supprime ses favoris
    void deleteByUserId(Long userId);

    List<Favori> findByUserIdOrderByDateAjoutDesc(Long userId);

    Optional<Favori> findByUserIdAndActiviteId(Long userId, Long activiteId);

    boolean existsByUserIdAndActiviteId(Long userId, Long activiteId);

    long countByActiviteId(Long activiteId);
}
