package com.travelmate.restaurants.controller;

import com.travelmate.restaurants.dto.ReanalyseDTO;
import com.travelmate.restaurants.dto.StatutIaDTO;
import com.travelmate.restaurants.service.AvisService;
import com.travelmate.restaurants.service.OllamaStatutService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/*
 * Endpoints IA du module 4, sous /api/restaurants/ia.
 * L'analyse de sentiment est faite à l'ajout d'un avis ; ces routes servent au bandeau
 * "Llama 3 / analyse locale" et au bouton de réanalyse.
 */
@RestController
@RequestMapping("/api/restaurants/ia")
public class IaController {

    private final OllamaStatutService ollamaStatutService;
    private final AvisService avisService;

    public IaController(OllamaStatutService ollamaStatutService, AvisService avisService) {
        this.ollamaStatutService = ollamaStatutService;
        this.avisService = avisService;
    }

    @GetMapping("/statut")
    public StatutIaDTO statut() {
        boolean disponible = ollamaStatutService.estDisponible();
        String message = disponible
                ? "Ollama est disponible : les avis sont analysés par " + ollamaStatutService.getModele() + "."
                : "Ollama est indisponible : l'analyse locale de secours est utilisée.";
        return new StatutIaDTO(disponible, ollamaStatutService.getModele(),
                ollamaStatutService.getUrl(), message);
    }

    @PostMapping("/reanalyser-avis")
    public ReanalyseDTO reanalyserAvis() {
        return avisService.reanalyserTous();
    }
}
