package com.travelmate.restaurants.mapper;

import com.travelmate.restaurants.dto.AvisDTO;
import com.travelmate.restaurants.dto.RestaurantDTO;
import com.travelmate.restaurants.entity.Avis;
import com.travelmate.restaurants.entity.Restaurant;
import com.travelmate.users.entity.User;

import java.util.ArrayList;

/** Transforme les entités JPA en DTO : on n'expose jamais directement les entités. */
public final class RestaurantMapper {

    private RestaurantMapper() {
    }

    public static RestaurantDTO versDto(Restaurant restaurant) {
        return new RestaurantDTO(
                restaurant.getId(),
                restaurant.getNom(),
                restaurant.getCuisine(),
                restaurant.getDescription(),
                restaurant.getAdresse(),
                restaurant.getVille(),
                restaurant.getPrixMoyen(),
                fourchettePrix(restaurant.getPrixMoyen()),
                new ArrayList<>(restaurant.getOptionsAlimentaires()),
                restaurant.getNoteMoyenne(),
                restaurant.getNombreAvis(),
                restaurant.getImageUrl()
        );
    }

    public static AvisDTO versDto(Avis avis) {
        return new AvisDTO(
                avis.getId(),
                avis.getRestaurant().getId(),
                nomAuteur(avis.getAuteur()),
                avis.getNote(),
                avis.getCommentaire(),
                avis.getSentiment().name(),
                avis.getScoreSentiment(),
                avis.getSourceSentiment(),
                avis.getDateCreation()
        );
    }

    // Le frontend affiche "Prénom Nom" ; l'email reste privé.
    private static String nomAuteur(User auteur) {
        String nom = (auteur.getFirstName() + " " + auteur.getLastName()).trim();
        return nom.isEmpty() ? auteur.getEmail() : nom;
    }

    private static String fourchettePrix(double prixMoyen) {
        if (prixMoyen <= 20) {
            return "€";
        }
        if (prixMoyen <= 40) {
            return "€€";
        }
        return "€€€";
    }
}
