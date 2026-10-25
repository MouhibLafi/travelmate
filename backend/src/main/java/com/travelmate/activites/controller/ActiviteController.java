package com.travelmate.activites.controller;

import com.travelmate.activites.dto.RecommandationDTO;
import com.travelmate.activites.model.Activite;
import com.travelmate.activites.model.Categorie;
import com.travelmate.activites.repository.ActiviteRepository;
import com.travelmate.activites.repository.CategorieRepository;
import com.travelmate.activites.service.ActiviteSpecifications;
import com.travelmate.activites.service.RecommandationService;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api")
public class ActiviteController {

    private final ActiviteRepository activiteRepository;
    private final CategorieRepository categorieRepository;
    private final RecommandationService recommandationService;

    public ActiviteController(ActiviteRepository activiteRepository,
                              CategorieRepository categorieRepository,
                              RecommandationService recommandationService) {
        this.activiteRepository = activiteRepository;
        this.categorieRepository = categorieRepository;
        this.recommandationService = recommandationService;
    }

    /**
     * Liste des activités avec recherche et filtres.
     * Exemple : /api/activites?destinationId=1&categorie=Culture&prixMax=30&dureeMax=180&q=musée&tri=prix
     */
    @GetMapping("/activites")
    public List<Activite> lister(@RequestParam(required = false) Long destinationId,
                                 @RequestParam(required = false) String categorie,
                                 @RequestParam(required = false) BigDecimal prixMin,
                                 @RequestParam(required = false) BigDecimal prixMax,
                                 @RequestParam(required = false) Integer dureeMax,
                                 @RequestParam(required = false) String q,
                                 @RequestParam(defaultValue = "nom") String tri) {
        String champ = switch (tri) {
            case "prix" -> "prix";
            case "duree" -> "dureeMinutes";
            default -> "nom";
        };
        return activiteRepository.findAll(
                ActiviteSpecifications.filtrer(destinationId, categorie, prixMin, prixMax, dureeMax, q),
                Sort.by(champ));
    }

    /**
     * Recommandations personnalisées (partie IA).
     * Exemple : /api/activites/recommandations?interets=Nature,Culture&destinationId=1&budgetMax=40&limite=5
     * Les centres d'intérêt proviennent du profil utilisateur (module 1).
     *
     * ⚠️ Doit être déclaré AVANT /activites/{id} pour éviter que "recommandations"
     * soit interprété comme un {id} par Spring MVC.
     */
    @GetMapping("/activites/recommandations")
    public List<RecommandationDTO> recommandations(@RequestParam List<String> interets,
                                                   @RequestParam(required = false) Long destinationId,
                                                   @RequestParam(required = false) BigDecimal budgetMax,
                                                   @RequestParam(defaultValue = "5") int limite) {
        return recommandationService.recommander(interets, destinationId, budgetMax, limite);
    }

    @GetMapping("/activites/{id}")
    public Activite detail(@PathVariable Long id) {
        return activiteRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Activité introuvable"));
    }

    @GetMapping("/categories")
    public List<Categorie> categories() {
        return categorieRepository.findAll(Sort.by("nom"));
    }
}
