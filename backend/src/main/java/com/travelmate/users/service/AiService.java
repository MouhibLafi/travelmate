package com.travelmate.users.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.travelmate.ai.OllamaClient;
import com.travelmate.users.dto.AiResponse;
import com.travelmate.users.entity.BudgetLevel;
import com.travelmate.users.entity.Interest;
import com.travelmate.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/*
 * TACHE IA DU MODULE 1
 * L'utilisateur se decrit en une phrase. On envoie cette phrase au modele llama3
 * (qui tourne en local avec Ollama) et il en deduit :
 *   - les centres d'interet (CULTURE, PLAGE, ...)
 *   - le niveau de budget (ECONOMIQUE, MOYEN, LUXE)
 *
 * Etapes :
 *   1. on construit un prompt (instructions + liste des valeurs autorisees + exemple)
 *   2. on appelle Ollama via le client commun OllamaClient (POST /api/generate)
 *   3. on lit le JSON renvoye par le modele et on garde seulement les valeurs valides
 */
@Service
public class AiService {

    private final OllamaClient ollamaClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AiService(OllamaClient ollamaClient) {
        this.ollamaClient = ollamaClient;
    }

    public AiResponse analyzeProfile(String description) {
        String prompt = buildPrompt(description);
        String modelAnswer = ollamaClient.generateJson(prompt);
        AiResponse answer = parseAnswer(modelAnswer);

        // Si le modele a simplement recopie la phrase, on ecrit nous-memes une explication
        if (sameText(answer.explanation(), description)) {
            answer = new AiResponse(answer.interests(), answer.budgetLevel(), answer.confidence(),
                    defaultExplanation(answer));
        }
        return answer;
    }

    private boolean sameText(String a, String b) {
        String cleanA = a.replaceAll("[^\\p{L}\\p{N}]", "").toLowerCase();
        String cleanB = b.replaceAll("[^\\p{L}\\p{N}]", "").toLowerCase();
        return cleanA.isEmpty() || cleanA.equals(cleanB);
    }

    private String defaultExplanation(AiResponse answer) {
        String interests = answer.interests().isEmpty()
                ? "aucun centre d'intérêt précis"
                : answer.interests().toString().replace("[", "").replace("]", "");
        return "D'après votre phrase : " + interests + ", budget " + answer.budgetLevel() + ".";
    }

    // ---------- 1. Le prompt ----------

    private String buildPrompt(String description) {
        return """
                Tu es l'assistant IA de TravelMate, une application de voyage.
                Un utilisateur se décrit en une phrase. Déduis ses centres d'intérêt et son niveau de budget.

                Centres d'intérêt autorisés (1 à 4, seulement ceux qui sont dans la phrase) :
                CULTURE (musées, art, théâtre), HISTOIRE (monuments, sites anciens, médinas),
                PLAGE (mer, soleil, baignade), NATURE (randonnée, montagne, désert, animaux),
                GASTRONOMIE (cuisine, restaurants, street food), AVENTURE (sensations fortes, plongée, escalade),
                SHOPPING (boutiques, souks, souvenirs), VIE_NOCTURNE (bars, clubs, soirées),
                SPORT (surf, vélo, ski, football), DETENTE (repos, spa, hammam, calme).

                Niveaux de budget autorisés (un seul) :
                ECONOMIQUE (petit budget, étudiant, pas cher), MOYEN (budget raisonnable, hôtel correct),
                LUXE (haut de gamme, hôtels 5 étoiles, sans compter).
                Si le budget n'est pas indiqué, choisis MOYEN avec une confiance faible.

                Réponds UNIQUEMENT avec un objet JSON contenant les clés
                "interests", "budgetLevel", "confidence" (entre 0 et 1) et "explanation".
                "explanation" est une phrase courte en français qui JUSTIFIE ton choix
                en citant les mots de l'utilisateur. Ne recopie jamais la phrase de l'utilisateur.

                Exemple :
                Phrase : "Je veux visiter des musées et des médinas sans trop dépenser"
                Réponse : {"interests": ["CULTURE", "HISTOIRE"], "budgetLevel": "ECONOMIQUE", "confidence": 0.85,
                 "explanation": "Les musées et les médinas indiquent la culture et l'histoire ; « sans trop dépenser » indique un petit budget."}

                Phrase de l'utilisateur : "%s"
                """.formatted(description.replace("\"", "'"));
    }

    // ---------- 2. L'appel a Ollama : voir ai/OllamaClient.java ----------

    // ---------- 3. Lecture de la reponse du modele ----------

    // "public" pour pouvoir le tester sans lancer Ollama (voir AiServiceTest)
    public AiResponse parseAnswer(String json) {
        try {
            JsonNode node = objectMapper.readTree(json);

            // Centres d'interet : on ignore les valeurs inventees par le modele
            List<Interest> interests = new ArrayList<>();
            for (JsonNode item : node.path("interests")) {
                Interest interest = toInterest(item.asText());
                if (interest != null && !interests.contains(interest) && interests.size() < 4) {
                    interests.add(interest);
                }
            }

            // Budget : MOYEN si la valeur n'est pas valide
            BudgetLevel budget = toBudget(node.path("budgetLevel").asText());
            double confidence = node.path("confidence").asDouble(0.5);
            if (budget == null) {
                budget = BudgetLevel.MOYEN;
                confidence = 0.3;
            }
            confidence = Math.max(0, Math.min(1, confidence)); // toujours entre 0 et 1

            String explanation = node.path("explanation").asText("");
            return new AiResponse(interests, budget, confidence, explanation);

        } catch (Exception e) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "Réponse de l'IA illisible, réessayez.");
        }
    }

    private Interest toInterest(String value) {
        try {
            return Interest.valueOf(value.trim().toUpperCase().replace(" ", "_"));
        } catch (IllegalArgumentException e) {
            return null; // valeur inconnue
        }
    }

    private BudgetLevel toBudget(String value) {
        try {
            return BudgetLevel.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
