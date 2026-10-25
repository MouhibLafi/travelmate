package com.travelmate.restaurants.repository;

import com.travelmate.restaurants.entity.Favori;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FavoriRepository extends JpaRepository<Favori, Long> {

    List<Favori> findByUtilisateurEmailOrderByDateAjoutDesc(String email);

    Optional<Favori> findByUtilisateurEmailAndRestaurantId(String email, Long restaurantId);

    @Query("select f.restaurant.id from Favori f where f.utilisateur.email = :email")
    List<Long> findRestaurantIdsByUtilisateurEmail(@Param("email") String email);

    void deleteByUtilisateurId(Long utilisateurId);
}
