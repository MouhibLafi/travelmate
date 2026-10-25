package com.travelmate.restaurants.repository;

import com.travelmate.restaurants.entity.Restaurant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface RestaurantRepository extends JpaRepository<Restaurant, Long>,
        JpaSpecificationExecutor<Restaurant> {

    // Valeurs distinctes utilisées pour construire les listes de filtres du frontend
    @Query("select distinct r.cuisine from Restaurant r order by r.cuisine")
    List<String> findCuisines();

    @Query("select distinct r.ville from Restaurant r order by r.ville")
    List<String> findVilles();

    @Query("select distinct o from Restaurant r join r.optionsAlimentaires o order by o")
    List<String> findOptionsAlimentaires();

    @Query("select min(r.prixMoyen) from Restaurant r")
    Double findPrixMinimum();

    @Query("select max(r.prixMoyen) from Restaurant r")
    Double findPrixMaximum();
}
