package com.travelmate.accommodation.repository;

import com.travelmate.accommodation.entity.Accommodation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface AccommodationRepository extends JpaRepository<Accommodation, Long>,
        JpaSpecificationExecutor<Accommodation> {

    @Query("select distinct a.city from Accommodation a order by a.city")
    List<String> findDistinctCities();
}
