package com.travelmate.trips.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.travelmate.ai.OllamaClient;
import com.travelmate.exception.ApiException;
import com.travelmate.trips.dto.TripSuggestion;
import com.travelmate.users.entity.BudgetLevel;
import com.travelmate.users.entity.Interest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/*
 * TACHE IA DU MODULE 2
 * L'utilisateur ecrit sa demande en texte libre ("5 jours a Rome, 800 €")
 * et l'IA remplit automatiquement le formulaire du voyage.
 *
 * Etapes :
 *   1. on construit un prompt : le modele doit seulement EXTRAIRE les informations de la phrase
 *   2. on appelle llama3 via le client commun OllamaClient
 *   3. on verifie la reponse et on CALCULE le reste en Java (plus fiable qu'un modele) :
 *        - date de debut absente ou passee  -> dans 7 jours
 *        - date de fin = debut + duree
 *        - niveau de budget = budget par jour (ECONOMIQUE < 80 €, MOYEN <= 200 €, LUXE au-dela) ;
 *          sans montant, on garde le niveau deduit des mots ("pas cher" -> ECONOMIQUE)
 *        - destination ecrite proprement ("ROME" -> "Rome")
 */
@Service
public class TripAiService {

    static final int DEFAULT_DURATION = 7;   // jours, si la duree n'est pas indiquee
    static final int MAX_DURATION = 60;

    private final OllamaClient ollamaClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public TripAiService(OllamaClient ollamaClient) {
        this.ollamaClient = ollamaClient;
    }

    public TripSuggestion fillForm(String text) {
        LocalDate today = LocalDate.now();
        String modelAnswer = ollamaClient.generateJson(buildPrompt(text, today));
        return parseAnswer(modelAnswer, today);
    }

    // ---------- 1. Le prompt ----------

    private String buildPrompt(String text, LocalDate today) {
        return """
                Tu es l'assistant IA de TravelMate, une application de voyage.
                Un utilisateur décrit le voyage qu'il veut faire. Extrais les informations de sa demande.
                Aujourd'hui nous sommes le %s.

                Réponds UNIQUEMENT avec un objet JSON contenant ces clés :
                - "destination" : la ville ou le pays (avec une majuscule), ou null si absent
                - "startDate" : la date de début au format AAAA-MM-JJ si elle est indiquée, sinon null
                - "durationDays" : le nombre de jours (nombre entier), ou null si absent
                - "budget" : le budget total en euros (nombre), ou null si absent
                - "budgetLevel" : seulement si aucun montant n'est donné mais que des mots l'indiquent :
                  ECONOMIQUE (pas cher, petit budget), MOYEN (raisonnable), LUXE (haut de gamme), sinon null
                - "interests" : 0 à 4 centres d'intérêt cités, choisis UNIQUEMENT dans cette liste :
                  CULTURE (musées, art), HISTOIRE (monuments, sites anciens, médinas), PLAGE (mer, baignade),
                  NATURE (randonnée, montagne, désert), GASTRONOMIE (cuisine, restaurants),
                  AVENTURE (sensations fortes, plongée, escalade), SHOPPING (boutiques, souks),
                  VIE_NOCTURNE (bars, soirées), SPORT (surf, vélo, ski), DETENTE (repos, spa, calme)
                - "confidence" : entre 0 et 1
                - "explanation" : une phrase courte en français qui justifie ton extraction

                N'invente rien : si une information n'est pas dans la demande, mets null.
                Une semaine = 7 jours, un week-end = 2 jours.

                Exemple :
                Demande : "Une semaine à Marrakech en avril pour visiter les souks, 600 euros"
                Réponse : {"destination": "Marrakech", "startDate": null, "durationDays": 7, "budget": 600,
                 "budgetLevel": null, "interests": ["SHOPPING"], "confidence": 0.85,
                 "explanation": "Marrakech pendant une semaine ; les souks indiquent le shopping ; budget de 600 €."}

                Demande de l'utilisateur : "%s"
                """.formatted(today, text.replace("\"", "'"));
    }

    // ---------- 2. L'appel a Ollama : voir ai/OllamaClient.java ----------

    // ---------- 3. Verification et calculs ----------

    // "public" et "today" en parametre pour pouvoir le tester sans Ollama (voir TripAiServiceTest)
    public TripSuggestion parseAnswer(String json, LocalDate today) {
        JsonNode node;
        try {
            node = objectMapper.readTree(json);
        } catch (Exception e) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "Réponse de l'IA illisible, réessayez.");
        }

        double confidence = Math.max(0, Math.min(1, node.path("confidence").asDouble(0.5)));

        // Destination : texte non vide, sinon null
        String destination = capitalize(text(node, "destination"));

        // Date de debut : valide et pas dans le passe, sinon dans 7 jours
        LocalDate startDate = toDate(text(node, "startDate"));
        if (startDate == null || startDate.isBefore(today)) {
            startDate = today.plusDays(7);
        }

        // Duree : entre 1 et 60 jours, sinon 7 jours avec une confiance plus faible
        int duration = node.path("durationDays").asInt(0);
        if (duration < 1 || duration > MAX_DURATION) {
            duration = DEFAULT_DURATION;
            confidence = Math.min(confidence, 0.5);
        }
        // "5 jours" du 20 au 24 : le jour de depart compte
        LocalDate endDate = startDate.plusDays(duration - 1);

        // Budget : nombre positif, sinon null
        Double budget = node.path("budget").isNumber() && node.path("budget").asDouble() > 0
                ? node.path("budget").asDouble()
                : null;

        // Centres d'interet : on ignore les valeurs inventees par le modele
        List<Interest> interests = new ArrayList<>();
        for (JsonNode item : node.path("interests")) {
            Interest interest = toInterest(item.asText());
            if (interest != null && !interests.contains(interest) && interests.size() < 4) {
                interests.add(interest);
            }
        }

        // Niveau de budget : calcule si on a un montant, sinon celui deduit des mots par le modele
        BudgetLevel budgetLevel = budget != null
                ? budgetLevel(budget, duration)
                : toBudgetLevel(text(node, "budgetLevel"));
        String explanation = node.path("explanation").asText("");

        return new TripSuggestion(destination, startDate, endDate, budget, interests, budgetLevel,
                confidence, explanation);
    }

    // Niveau de budget calcule a partir du budget par jour (null si pas de budget)
    static BudgetLevel budgetLevel(Double budget, int duration) {
        if (budget == null) {
            return null;
        }
        double perDay = budget / duration;
        if (perDay < 80) return BudgetLevel.ECONOMIQUE;
        if (perDay <= 200) return BudgetLevel.MOYEN;
        return BudgetLevel.LUXE;
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        if (!value.isTextual() || value.asText().isBlank() || value.asText().equalsIgnoreCase("null")) {
            return null;
        }
        return value.asText().trim();
    }

    // "ROME" ou "new york" -> "Rome", "New York"
    static String capitalize(String value) {
        if (value == null) {
            return null;
        }
        StringBuilder result = new StringBuilder();
        boolean newWord = true;
        for (char c : value.toLowerCase().toCharArray()) {
            result.append(newWord ? Character.toUpperCase(c) : c);
            newWord = c == ' ' || c == '-';
        }
        return result.toString();
    }

    private BudgetLevel toBudgetLevel(String value) {
        if (value == null) {
            return null;
        }
        try {
            return BudgetLevel.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null; // valeur inconnue
        }
    }

    private LocalDate toDate(String value) {
        if (value == null) {
            return null;
        }
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            return null; // date mal ecrite par le modele
        }
    }

    private Interest toInterest(String value) {
        try {
            return Interest.valueOf(value.trim().toUpperCase().replace(" ", "_"));
        } catch (IllegalArgumentException e) {
            return null; // valeur inconnue
        }
    }
}
