package com.travelmate.restaurants.controller;

import com.travelmate.restaurants.dto.RestaurantDTO;
import com.travelmate.restaurants.service.FavoriService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;

/*
 * Favoris de l'utilisateur connecté, sous /api/restaurants/favoris.
 * L'utilisateur est identifié par le JWT (Principal), pas par un header.
 */
@RestController
@RequestMapping("/api/restaurants/favoris")
public class FavoriController {

    private final FavoriService favoriService;

    public FavoriController(FavoriService favoriService) {
        this.favoriService = favoriService;
    }

    @GetMapping
    public List<RestaurantDTO> lister(Principal principal) {
        return favoriService.lister(principal.getName());
    }

    // GET /api/restaurants/favoris/ids : utilisé par le catalogue pour colorer les cœurs
    @GetMapping("/ids")
    public List<Long> ids(Principal principal) {
        return favoriService.ids(principal.getName());
    }

    @PostMapping("/{restaurantId:[0-9]+}")
    @ResponseStatus(HttpStatus.CREATED)
    public RestaurantDTO ajouter(Principal principal, @PathVariable Long restaurantId) {
        return favoriService.ajouter(principal.getName(), restaurantId);
    }

    @DeleteMapping("/{restaurantId:[0-9]+}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void supprimer(Principal principal, @PathVariable Long restaurantId) {
        favoriService.supprimer(principal.getName(), restaurantId);
    }
}
