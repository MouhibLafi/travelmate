package com.travelmate.trips.repository;

import com.travelmate.trips.entity.Trip;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TripRepository extends JpaRepository<Trip, Long> {

    // Les voyages d'un utilisateur, retrouve par son email (celui du jeton JWT)
    List<Trip> findByUserEmailOrderByStartDateAsc(String email);

    List<Trip> findByUserId(Long userId);
}
