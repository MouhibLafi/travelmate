package com.travelmate.activites.controller;

import com.travelmate.activites.model.Destination;
import com.travelmate.activites.repository.DestinationRepository;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/destinations")
public class DestinationController {

    private final DestinationRepository repository;

    public DestinationController(DestinationRepository repository) {
        this.repository = repository;
    }

    /** Catalogue des destinations, avec recherche optionnelle (nom, pays, tags). */
    @GetMapping
    public List<Destination> lister(@RequestParam(required = false) String q) {
        if (q == null || q.isBlank()) {
            return repository.findAll();
        }
        String motif = "%" + q.toLowerCase() + "%";
        Specification<Destination> spec = (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("nom")), motif),
                cb.like(cb.lower(root.get("pays")), motif),
                cb.like(cb.lower(root.get("tags")), motif));
        return repository.findAll(spec);
    }

    @GetMapping("/{id}")
    public Destination detail(@PathVariable Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Destination introuvable"));
    }
}
