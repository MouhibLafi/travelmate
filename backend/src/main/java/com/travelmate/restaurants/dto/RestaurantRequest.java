package com.travelmate.restaurants.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Set;

// Donnees du formulaire admin "Ajouter / modifier un restaurant"
public record RestaurantRequest(
        @NotBlank(message = "Le nom est obligatoire") @Size(max = 120) String nom,
        @NotBlank(message = "La cuisine est obligatoire") @Size(max = 60) String cuisine,
        @NotBlank(message = "La description est obligatoire") @Size(max = 1000) String description,
        @NotBlank(message = "L'adresse est obligatoire") @Size(max = 200) String adresse,
        @NotBlank(message = "La ville est obligatoire") @Size(max = 80) String ville,
        @DecimalMin(value = "1", message = "Le prix moyen doit être d'au moins 1 €")
        @DecimalMax(value = "1000", message = "Le prix moyen doit être d'au plus 1000 €") double prixMoyen,
        @Size(max = 500) String imageUrl,
        Set<String> optionsAlimentaires
) {
}
