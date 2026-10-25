package com.travelmate.activites.repository;

import com.travelmate.activites.model.Categorie;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CategorieRepository extends JpaRepository<Categorie, Long> {
    Optional<Categorie> findByNomIgnoreCase(String nom);
}
