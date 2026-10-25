package com.travelmate.activites.controller;

import com.travelmate.activites.model.Activite;
import com.travelmate.activites.model.Favori;
import com.travelmate.activites.repository.ActiviteRepository;
import com.travelmate.activites.repository.FavoriRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Activités favorites. L'identifiant utilisateur est passé en paramètre (userId) ;
 * à remplacer par l'utilisateur authentifié quand le module 1 (JWT) sera intégré.
 */
@RestController
@RequestMapping("/api/favoris")
public class FavoriController {

    private final FavoriRepository favoriRepository;
    private final ActiviteRepository activiteRepository;

    public FavoriController(FavoriRepository favoriRepository, ActiviteRepository activiteRepository) {
        this.favoriRepository = favoriRepository;
        this.activiteRepository = activiteRepository;
    }

    @GetMapping
    public List<Activite> lister(@RequestParam Long userId) {
        return favoriRepository.findByUserIdOrderByDateAjoutDesc(userId).stream()
                .map(Favori::getActivite)
                .toList();
    }

    @PostMapping("/{activiteId}")
    @ResponseStatus(HttpStatus.CREATED)
    public void ajouter(@PathVariable Long activiteId, @RequestParam Long userId) {
        Activite activite = activiteRepository.findById(activiteId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Activité introuvable"));
        if (!favoriRepository.existsByUserIdAndActiviteId(userId, activiteId)) {
            favoriRepository.save(new Favori(userId, activite));
        }
    }

    @DeleteMapping("/{activiteId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void retirer(@PathVariable Long activiteId, @RequestParam Long userId) {
        favoriRepository.findByUserIdAndActiviteId(userId, activiteId).ifPresent(favoriRepository::delete);
    }

    /** Pour afficher l'état du cœur dans l'interface. */
    @GetMapping("/{activiteId}/existe")
    public boolean estFavori(@PathVariable Long activiteId, @RequestParam Long userId) {
        return favoriRepository.existsByUserIdAndActiviteId(userId, activiteId);
    }
}
