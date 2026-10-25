package com.travelmate.activites.repository;

import com.travelmate.activites.model.Activite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface ActiviteRepository extends JpaRepository<Activite, Long>, JpaSpecificationExecutor<Activite> {
    List<Activite> findByDestinationId(Long destinationId);
}
