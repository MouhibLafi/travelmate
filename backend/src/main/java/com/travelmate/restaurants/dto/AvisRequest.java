package com.travelmate.restaurants.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/*
 * L'auteur n'est plus demandé dans le formulaire : c'est l'utilisateur connecté.
 * Le frontend envoie seulement la note et le commentaire.
 */
public record AvisRequest(
        @Min(value = 1, message = "La note doit être comprise entre 1 et 5")
        @Max(value = 5, message = "La note doit être comprise entre 1 et 5")
        int note,

        @NotBlank(message = "Le commentaire est obligatoire")
        @Size(min = 3, max = 1000, message = "Le commentaire doit contenir entre 3 et 1000 caractères")
        String commentaire
) {
}
