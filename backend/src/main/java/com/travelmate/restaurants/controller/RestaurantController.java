package com.travelmate.restaurants.controller;

import com.travelmate.restaurants.dto.AnalyseAvisDTO;
import com.travelmate.restaurants.dto.AvisDTO;
import com.travelmate.restaurants.dto.AvisRequest;
import com.travelmate.restaurants.dto.MetaFiltresDTO;
import com.travelmate.restaurants.dto.RecommandationDTO;
import com.travelmate.restaurants.dto.RestaurantDTO;
import com.travelmate.restaurants.service.AvisService;
import com.travelmate.restaurants.service.RecommandationService;
import com.travelmate.restaurants.service.RestaurantService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;

/*
 * API du module 4, sous /api/restaurants.
 * La sécurité est déjà assurée par le JWT du module 1 : toutes les routes /api/** sont privées.
 */
@RestController
@RequestMapping("/api/restaurants")
public class RestaurantController {

    private final RestaurantService restaurantService;
    private final AvisService avisService;
    private final RecommandationService recommandationService;

    public RestaurantController(RestaurantService restaurantService,
                                AvisService avisService,
                                RecommandationService recommandationService) {
        this.restaurantService = restaurantService;
        this.avisService = avisService;
        this.recommandationService = recommandationService;
    }

    // GET /api/restaurants?q=tunis&cuisine=Tunisienne&prixMax=30&options=Halal
    @GetMapping
    public List<RestaurantDTO> rechercher(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String cuisine,
            @RequestParam(required = false) String ville,
            @RequestParam(required = false) Double prixMin,
            @RequestParam(required = false) Double prixMax,
            @RequestParam(required = false) Double noteMin,
            @RequestParam(required = false) List<String> options) {
        return restaurantService.rechercher(q, cuisine, ville, prixMin, prixMax, noteMin, options);
    }

    // GET /api/restaurants/filtres : listes pour construire les menus déroulants
    @GetMapping("/filtres")
    public MetaFiltresDTO filtres() {
        return restaurantService.filtres();
    }

    // GET /api/restaurants/recommandations?budgetMax=30&ville=Tunis
    // Le budget est optionnel : sans valeur, on utilise le niveau du profil de l'utilisateur.
    @GetMapping("/recommandations")
    public List<RecommandationDTO> recommander(
            Principal principal,
            @RequestParam(required = false) String cuisine,
            @RequestParam(required = false) Double budgetMax,
            @RequestParam(required = false) String ville,
            @RequestParam(required = false) List<String> options,
            @RequestParam(defaultValue = "5") int limite) {
        return recommandationService.recommander(principal.getName(), cuisine, budgetMax, options, ville, limite);
    }

    @GetMapping("/{id:[0-9]+}")
    public RestaurantDTO detail(@PathVariable Long id) {
        return restaurantService.detail(id);
    }

    @GetMapping("/{id:[0-9]+}/avis")
    public List<AvisDTO> listerAvis(@PathVariable Long id) {
        return avisService.lister(id);
    }

    @PostMapping("/{id:[0-9]+}/avis")
    @ResponseStatus(HttpStatus.CREATED)
    public AvisDTO ajouterAvis(Principal principal, @PathVariable Long id,
                               @Valid @RequestBody AvisRequest requete) {
        return avisService.ajouter(id, principal.getName(), requete);
    }

    @GetMapping("/{id:[0-9]+}/analyse-avis")
    public AnalyseAvisDTO analyserAvis(@PathVariable Long id) {
        return avisService.analyser(id);
    }
}
