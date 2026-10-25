package com.travelmate.restaurants.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/*
 * Vérifie si Ollama répond, pour afficher le bon mode dans l'interface
 * (Llama 3 ou analyse locale de secours).
 * On ne passe pas par OllamaClient ici : il lève une exception quand Ollama est arrêté,
 * alors que le bandeau de statut doit simplement afficher "indisponible".
 */
@Service
public class OllamaStatutService {

    private static final Logger log = LoggerFactory.getLogger(OllamaStatutService.class);

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(2))
            .build();

    @Value("${ollama.url}")
    private String url;

    @Value("${ollama.model}")
    private String modele;

    private volatile boolean cacheDisponible = false;
    private volatile long expirationCache = 0L;

    public String getModele() {
        return modele;
    }

    public String getUrl() {
        return url;
    }

    /** Résultat gardé 30 secondes pour éviter d'appeler Ollama à chaque affichage de page. */
    public boolean estDisponible() {
        long maintenant = System.currentTimeMillis();
        if (maintenant < expirationCache) {
            return cacheDisponible;
        }
        boolean disponible = testerDisponibilite();
        cacheDisponible = disponible;
        expirationCache = maintenant + 30_000L;
        return disponible;
    }

    private boolean testerDisponibilite() {
        try {
            HttpRequest requete = HttpRequest.newBuilder()
                    .uri(URI.create(url + "/api/tags"))
                    .timeout(Duration.ofSeconds(2))
                    .GET()
                    .build();
            HttpResponse<String> reponse = httpClient.send(requete, HttpResponse.BodyHandlers.ofString());
            return reponse.statusCode() == 200;
        } catch (Exception e) {
            log.info("Ollama indisponible sur {} ({})", url, e.getClass().getSimpleName());
            return false;
        }
    }
}
