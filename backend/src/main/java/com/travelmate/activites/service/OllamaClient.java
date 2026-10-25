package com.travelmate.activites.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.travelmate.activites.dto.RecommandationDTO;
import com.travelmate.activites.model.Activite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Appelle l'API locale d'Ollama (modèle Llama 3). */
@Component
public class OllamaClient {

    private static final Logger log = LoggerFactory.getLogger(OllamaClient.class);

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String model;

    public OllamaClient(@Value("${ollama.base-url}") String baseUrl,
                        @Value("${ollama.model}") String model,
                        @Value("${ollama.timeout-seconds}") int timeoutSeconds,
                        ObjectMapper objectMapper) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(timeoutSeconds));
        this.restClient = RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
        this.objectMapper = objectMapper;
        this.model = model;
    }

    /**
     * Envoie un prompt à Llama 3 et renvoie la réponse JSON parsée,
     * ou null si Ollama est indisponible / la réponse est invalide.
     */
    public JsonNode genererJson(String prompt) {
        try {
            Map<String, Object> corps = Map.of(
                    "model", model,
                    "prompt", prompt,
                    "stream", false,
                    "format", "json",
                    "options", Map.of("temperature", 0.3)
            );
            String reponse = restClient.post()
                    .uri("/api/generate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(corps)
                    .retrieve()
                    .body(String.class);

            JsonNode racine = objectMapper.readTree(reponse);
            String texte = racine.path("response").asText("");
            return objectMapper.readTree(texte);
        } catch (Exception e) {
            log.warn("Ollama indisponible ou réponse invalide : {}", e.getMessage());
            return null;
        }
    }

    /**
     * Demande à Llama 3 de reclasser les activités candidates selon les centres d'intérêt
     * et le budget de l'utilisateur. Retourne un JsonNode
     * {@code {"recommandations":[{"id":123,"raison":"..."}]}} ou {@code null} si Ollama
     * est indisponible ou la réponse est invalide.
     */
    public JsonNode genererRecommandations(List<RecommandationDTO> candidates,
                                           List<String> interets,
                                           BigDecimal budgetMax) {
        String liste = candidates.stream()
                .map(r -> {
                    Activite a = r.activite();
                    return "{\"id\":" + a.getId()
                            + ",\"nom\":\"" + echapper(a.getNom()) + "\""
                            + ",\"categorie\":\"" + echapper(a.getCategorie().getNom()) + "\""
                            + ",\"prix\":" + a.getPrix()
                            + ",\"dureeMinutes\":" + a.getDureeMinutes()
                            + ",\"score\":" + r.score() + "}";
                })
                .collect(Collectors.joining(",\n"));

        String prompt = """
                Tu es l'assistant de recommandation de l'application de voyage TravelMate.
                Centres d'intérêt de l'utilisateur : %s
                Budget maximum par activité : %s

                Voici des activités candidates (JSON) avec un score de pertinence calculé :
                [%s]

                Tâche : classe ces activités de la plus à la moins adaptée aux centres d'intérêt et au budget,
                et écris pour chacune une raison courte (une phrase, en français, 20 mots maximum).
                Utilise UNIQUEMENT les id fournis. Réponds uniquement avec ce JSON :
                {"recommandations":[{"id":123,"raison":"..."}]}
                """.formatted(
                interets.isEmpty() ? "non précisés" : String.join(", ", interets),
                budgetMax == null ? "non précisé" : budgetMax + " €",
                liste);

        return genererJson(prompt);
    }

    private static String echapper(String s) {
        return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
