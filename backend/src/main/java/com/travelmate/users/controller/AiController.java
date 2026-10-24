package com.travelmate.users.controller;

import com.travelmate.users.dto.AiRequest;
import com.travelmate.users.dto.AiResponse;
import com.travelmate.users.service.AiService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final AiService aiService;

    public AiController(AiService aiService) {
        this.aiService = aiService;
    }

    // POST /api/ai/analyze-profile
    // body    : { "description": "J'adore la plage et la bonne cuisine, petit budget" }
    // reponse : { "interests": ["PLAGE","GASTRONOMIE"], "budgetLevel": "ECONOMIQUE", "confidence": 0.9, ... }
    // Rien n'est enregistre ici : l'utilisateur valide d'abord, puis le front appelle PUT /api/users/me/preferences
    @PostMapping("/analyze-profile")
    public AiResponse analyzeProfile(@Valid @RequestBody AiRequest request) {
        return aiService.analyzeProfile(request.description());
    }
}
