package com.travelmate.restaurants.controller;

import com.travelmate.restaurants.dto.RestaurantDTO;
import com.travelmate.restaurants.dto.RestaurantRequest;
import com.travelmate.restaurants.service.RestaurantAdminService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Gestion du catalogue des restaurants : reservee aux administrateurs.
// Pas de regle a ajouter : SecurityConfig (module 1) reserve deja /api/admin/** au role ADMIN.
@RestController
@RequestMapping("/api/admin/restaurants")
public class AdminRestaurantController {

    private final RestaurantAdminService adminService;

    public AdminRestaurantController(RestaurantAdminService adminService) {
        this.adminService = adminService;
    }

    // GET /api/admin/restaurants
    @GetMapping
    public List<RestaurantDTO> lister() {
        return adminService.lister();
    }

    // POST /api/admin/restaurants
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RestaurantDTO creer(@Valid @RequestBody RestaurantRequest requete) {
        return adminService.creer(requete);
    }

    // PUT /api/admin/restaurants/3
    @PutMapping("/{id:[0-9]+}")
    public RestaurantDTO modifier(@PathVariable Long id, @Valid @RequestBody RestaurantRequest requete) {
        return adminService.modifier(id, requete);
    }

    // DELETE /api/admin/restaurants/3  (supprime aussi ses avis et ses favoris)
    @DeleteMapping("/{id:[0-9]+}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void supprimer(@PathVariable Long id) {
        adminService.supprimer(id);
    }
}
