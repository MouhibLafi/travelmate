package com.travelmate.restaurants.service;

import com.travelmate.restaurants.dto.RestaurantDTO;
import com.travelmate.restaurants.entity.Favori;
import com.travelmate.restaurants.entity.Restaurant;
import com.travelmate.restaurants.mapper.RestaurantMapper;
import com.travelmate.restaurants.repository.FavoriRepository;
import com.travelmate.users.entity.User;
import com.travelmate.users.event.UserDeletedEvent;
import com.travelmate.users.service.UserService;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class FavoriService {

    private final FavoriRepository favoriRepository;
    private final RestaurantService restaurantService;
    private final UserService userService;

    public FavoriService(FavoriRepository favoriRepository,
                         RestaurantService restaurantService,
                         UserService userService) {
        this.favoriRepository = favoriRepository;
        this.restaurantService = restaurantService;
        this.userService = userService;
    }

    @Transactional(readOnly = true)
    public List<RestaurantDTO> lister(String email) {
        return favoriRepository.findByUtilisateurEmailOrderByDateAjoutDesc(email).stream()
                .map(favori -> RestaurantMapper.versDto(favori.getRestaurant()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Long> ids(String email) {
        return favoriRepository.findRestaurantIdsByUtilisateurEmail(email);
    }

    @Transactional
    public RestaurantDTO ajouter(String email, Long restaurantId) {
        Restaurant restaurant = restaurantService.trouverEntite(restaurantId);
        return favoriRepository.findByUtilisateurEmailAndRestaurantId(email, restaurantId)
                .map(favori -> RestaurantMapper.versDto(favori.getRestaurant()))
                .orElseGet(() -> {
                    User utilisateur = userService.findByEmail(email);
                    Favori favori = favoriRepository.save(new Favori(utilisateur, restaurant));
                    return RestaurantMapper.versDto(favori.getRestaurant());
                });
    }

    @Transactional
    public void supprimer(String email, Long restaurantId) {
        favoriRepository.findByUtilisateurEmailAndRestaurantId(email, restaurantId)
                .ifPresent(favoriRepository::delete);
    }

    // Appelé par le module 1 avant la suppression d'un compte
    @EventListener
    @Transactional
    public void onUserDeleted(UserDeletedEvent event) {
        favoriRepository.deleteByUtilisateurId(event.userId());
    }
}
