package com.travelmate.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.travelmate.exception.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

/*
 * Client Ollama COMMUN a tous les modules (llama3 en local).
 * Chaque module ecrit son prompt, appelle generateJson(prompt), puis verifie la reponse.
 * Exemple : users/service/AiService.java (module 1).
 */
@Component
public class OllamaClient {

    @Value("${ollama.url}")
    private String ollamaUrl;

    @Value("${ollama.model}")
    private String model;

    // Envoie le prompt a Ollama et renvoie le texte JSON genere par le modele
    public String generateJson(String prompt) {
        // Corps de la requete : POST http://localhost:11434/api/generate
        Map<String, Object> body = Map.of(
                "model", model,
                "prompt", prompt,
                "stream", false,                      // on veut la reponse complete en une fois
                "format", "json",                     // on force le modele a repondre en JSON
                "options", Map.of("temperature", 0)   // reponses stables (pas de hasard)
        );

        try {
            JsonNode response = RestClient.create(ollamaUrl)
                    .post()
                    .uri("/api/generate")
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);

            // Ollama met le texte genere par le modele dans le champ "response"
            return response.get("response").asText();
        } catch (Exception e) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE,
                    "L'IA ne répond pas. Vérifiez qu'Ollama est lancé et que le modèle est installé : ollama pull " + model);
        }
    }
}
