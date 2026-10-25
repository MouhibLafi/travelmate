package com.travelmate.restaurants.service;

import com.travelmate.restaurants.dto.MetaFiltresDTO;
import com.travelmate.restaurants.dto.RestaurantDTO;
import com.travelmate.restaurants.entity.Restaurant;
import com.travelmate.restaurants.mapper.RestaurantMapper;
import com.travelmate.restaurants.repository.RestaurantRepository;
import com.travelmate.restaurants.repository.RestaurantSpecifications;
import com.travelmate.exception.ApiException;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class RestaurantService {

    private final RestaurantRepository restaurantRepository;

    public RestaurantService(RestaurantRepository restaurantRepository) {
        this.restaurantRepository = restaurantRepository;
    }

    public List<RestaurantDTO> rechercher(String recherche, String cuisine, String ville,
                                          Double prixMin, Double prixMax, Double noteMin,
                                          List<String> options) {
        Specification<Restaurant> specification = RestaurantSpecifications.avecFiltres(
                recherche, cuisine, ville, prixMin, prixMax, noteMin, options);

        // Les mieux notés d'abord ; à note égale, ordre alphabétique
        Sort tri = Sort.by(Sort.Order.desc("noteMoyenne"), Sort.Order.asc("nom"));
        return restaurantRepository.findAll(specification, tri).stream()
                .map(RestaurantMapper::versDto)
                .toList();
    }

    public RestaurantDTO detail(Long id) {
        return RestaurantMapper.versDto(trouverEntite(id));
    }

    public Restaurant trouverEntite(Long id) {
        return restaurantRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Restaurant introuvable"));
    }

    public MetaFiltresDTO filtres() {
        Double prixMin = restaurantRepository.findPrixMinimum();
        Double prixMax = restaurantRepository.findPrixMaximum();
        return new MetaFiltresDTO(
                restaurantRepository.findCuisines(),
                restaurantRepository.findVilles(),
                restaurantRepository.findOptionsAlimentaires(),
                prixMin == null ? 0 : arrondir(prixMin),
                prixMax == null ? 0 : arrondir(prixMax)
        );
    }

    private double arrondir(double valeur) {
        return Math.round(valeur * 10.0) / 10.0;
    }
}
