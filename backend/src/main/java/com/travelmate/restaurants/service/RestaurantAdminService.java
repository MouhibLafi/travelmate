package com.travelmate.restaurants.service;

import com.travelmate.restaurants.dto.RestaurantDTO;
import com.travelmate.restaurants.dto.RestaurantRequest;
import com.travelmate.restaurants.entity.Restaurant;
import com.travelmate.restaurants.mapper.RestaurantMapper;
import com.travelmate.restaurants.repository.AvisRepository;
import com.travelmate.restaurants.repository.FavoriRepository;
import com.travelmate.restaurants.repository.RestaurantRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;

// Gestion du catalogue par l'administrateur : ajouter, modifier, supprimer un restaurant.
// La note moyenne et le nombre d'avis ne sont jamais saisis : ils viennent des avis des voyageurs.
@Service
public class RestaurantAdminService {

    private final RestaurantRepository restaurantRepository;
    private final RestaurantService restaurantService;
    private final AvisRepository avisRepository;
    private final FavoriRepository favoriRepository;

    public RestaurantAdminService(RestaurantRepository restaurantRepository, RestaurantService restaurantService,
                                  AvisRepository avisRepository, FavoriRepository favoriRepository) {
        this.restaurantRepository = restaurantRepository;
        this.restaurantService = restaurantService;
        this.avisRepository = avisRepository;
        this.favoriRepository = favoriRepository;
    }

    // Tous les restaurants, par ordre alphabetique
    @Transactional(readOnly = true)
    public List<RestaurantDTO> lister() {
        return restaurantRepository.findAll(Sort.by("nom")).stream()
                .map(RestaurantMapper::versDto)
                .toList();
    }

    @Transactional
    public RestaurantDTO creer(RestaurantRequest requete) {
        Restaurant restaurant = new Restaurant();
        remplir(restaurant, requete);
        return RestaurantMapper.versDto(restaurantRepository.save(restaurant));
    }

    @Transactional
    public RestaurantDTO modifier(Long id, RestaurantRequest requete) {
        Restaurant restaurant = restaurantService.trouverEntite(id);
        remplir(restaurant, requete);
        return RestaurantMapper.versDto(restaurantRepository.save(restaurant));
    }

    // On supprime d'abord les avis et les favoris du restaurant (cles etrangeres), puis le restaurant
    @Transactional
    public void supprimer(Long id) {
        Restaurant restaurant = restaurantService.trouverEntite(id);
        avisRepository.deleteByRestaurantId(id);
        favoriRepository.deleteByRestaurantId(id);
        restaurantRepository.delete(restaurant);
    }

    private void remplir(Restaurant restaurant, RestaurantRequest requete) {
        restaurant.setNom(requete.nom().trim());
        restaurant.setCuisine(requete.cuisine().trim());
        restaurant.setDescription(requete.description().trim());
        restaurant.setAdresse(requete.adresse().trim());
        restaurant.setVille(requete.ville().trim());
        restaurant.setPrixMoyen(requete.prixMoyen());
        restaurant.setImageUrl(requete.imageUrl() == null || requete.imageUrl().isBlank() ? null : requete.imageUrl().trim());
        restaurant.setOptionsAlimentaires(requete.optionsAlimentaires() == null
                ? new LinkedHashSet<>() : new LinkedHashSet<>(requete.optionsAlimentaires()));
    }
}
