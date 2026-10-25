package com.travelmate.restaurants.repository;

import com.travelmate.restaurants.entity.Avis;
import com.travelmate.restaurants.entity.Sentiment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AvisRepository extends JpaRepository<Avis, Long> {

    List<Avis> findByRestaurantIdOrderByDateCreationDesc(Long restaurantId);

    @Query("select avg(a.note) from Avis a where a.restaurant.id = :restaurantId")
    Double moyenneNotes(@Param("restaurantId") Long restaurantId);

    long countByRestaurantId(Long restaurantId);

    long countByRestaurantIdAndSentiment(Long restaurantId, Sentiment sentiment);

    // Utilisé quand un utilisateur supprime son compte (voir AvisService.onUserDeleted)
    List<Avis> findByAuteurId(Long auteurId);

    void deleteByAuteurId(Long auteurId);
}
