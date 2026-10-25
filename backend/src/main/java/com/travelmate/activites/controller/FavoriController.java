package com.travelmate.activites.controller;

import com.travelmate.activites.model.Activite;
import com.travelmate.activites.model.Favori;
import com.travelmate.activites.repository.ActiviteRepository;
import com.travelmate.activites.repository.FavoriRepository;
import com.travelmate.users.event.UserDeletedEvent;
import com.travelmate.users.service.UserService;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.List;

/**
 * Activités favorites de l'utilisateur CONNECTÉ.
 * Intégration avec le module 1 : l'utilisateur n'est plus passé en paramètre (userId),
 * il est lu dans le jeton JWT (principal.getName() = email).
 */
@RestController("activiteFavoriController")   // integration : nom distinct du FavoriController du module 4
@RequestMapping("/api/favoris")
public class FavoriController {

    private final FavoriRepository favoriRepository;
    private final ActiviteRepository activiteRepository;
    private final UserService userService;

    public FavoriController(FavoriRepository favoriRepository, ActiviteRepository activiteRepository,
                            UserService userService) {
        this.favoriRepository = favoriRepository;
        this.activiteRepository = activiteRepository;
        this.userService = userService;
    }

    @GetMapping
    public List<Activite> lister(Principal principal) {
        return favoriRepository.findByUserIdOrderByDateAjoutDesc(userId(principal)).stream()
                .map(Favori::getActivite)
                .toList();
    }

    @PostMapping("/{activiteId}")
    @ResponseStatus(HttpStatus.CREATED)
    public void ajouter(@PathVariable Long activiteId, Principal principal) {
        Long userId = userId(principal);
        Activite activite = activiteRepository.findById(activiteId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Activité introuvable"));
        if (!favoriRepository.existsByUserIdAndActiviteId(userId, activiteId)) {
            favoriRepository.save(new Favori(userId, activite));
        }
    }

    @DeleteMapping("/{activiteId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void retirer(@PathVariable Long activiteId, Principal principal) {
        favoriRepository.findByUserIdAndActiviteId(userId(principal), activiteId).ifPresent(favoriRepository::delete);
    }

    /** Pour afficher l'état du cœur dans l'interface. */
    @GetMapping("/{activiteId}/existe")
    public boolean estFavori(@PathVariable Long activiteId, Principal principal) {
        return favoriRepository.existsByUserIdAndActiviteId(userId(principal), activiteId);
    }

    /** Intégration : quand le module 1 supprime un utilisateur, on supprime ses favoris. */
    @EventListener
    @Transactional
    public void onUserDeleted(UserDeletedEvent event) {
        favoriRepository.deleteByUserId(event.userId());
    }

    // Identifiant de l'utilisateur connecté (module 1)
    private Long userId(Principal principal) {
        return userService.findByEmail(principal.getName()).getId();
    }
}
