package com.travelmate.itinerary.repository;

import com.travelmate.itinerary.entity.Itinerary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ItineraryRepository extends JpaRepository<Itinerary, Long> {

    // Les itineraires d'un utilisateur, retrouve par son email (celui du jeton JWT)
    List<Itinerary> findByUserEmailOrderByCreatedAtDesc(String email);

    List<Itinerary> findByUserId(Long userId);

    List<Itinerary> findByTripId(Long tripId);
}
