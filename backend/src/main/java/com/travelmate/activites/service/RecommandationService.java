package com.travelmate.activites.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.travelmate.activites.dto.RecommandationDTO;
import com.travelmate.activites.model.Activite;
import com.travelmate.activites.repository.ActiviteRepository;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Système de recommandation en deux étapes :
 * 1) filtrage par contenu : score (0-100) entre les centres d'intérêt et l'activité ;
 * 2) Llama 3 (Ollama) : reclasse les meilleures candidates et rédige une explication.
 * Si Ollama est indisponible, le classement de l'étape 1 est utilisé tel quel.
 */
@Service("activiteRecommandationService")   // integration : nom distinct du RecommandationService du module 4
public class RecommandationService {

    private static final int POIDS_CATEGORIE = 60;
    private static final int POIDS_TAGS = 25;
    private static final int POIDS_BUDGET = 15;
    private static final int NB_CANDIDATES_POUR_LLM = 10;

    private final ActiviteRepository activiteRepository;
    private final OllamaClient ollamaClient;

    public RecommandationService(ActiviteRepository activiteRepository, OllamaClient ollamaClient) {
        this.activiteRepository = activiteRepository;
        this.ollamaClient = ollamaClient;
    }

    public List<RecommandationDTO> recommander(List<String> interets, Long destinationId,
                                               BigDecimal budgetMaxParActivite, int limite) {
        Set<String> interetsNormalises = interets.stream()
                .map(RecommandationService::normaliser)
                .filter(s -> !s.isBlank())
                .collect(Collectors.toCollection(LinkedHashSet::new));

        List<Activite> candidates = activiteRepository.findAll(
                Specification.where(ActiviteSpecifications.filtrer(destinationId, null, null, null, null, null)));

        // Étape 1 : scoring par contenu
        List<RecommandationDTO> classees = candidates.stream()
                .map(a -> new RecommandationDTO(a, calculerScore(a, interetsNormalises, budgetMaxParActivite), null))
                .sorted(Comparator.comparingInt(RecommandationDTO::score).reversed()
                        .thenComparing(r -> r.activite().getPrix()))
                .limit(Math.max(limite, NB_CANDIDATES_POUR_LLM))
                .toList();

        // Étape 2 : reclassement + explications par Llama 3
        List<RecommandationDTO> finales = raffinerAvecLlm(classees, interets, budgetMaxParActivite);

        return finales.stream().limit(limite).toList();
    }

    int calculerScore(Activite a, Set<String> interets, BigDecimal budgetMax) {
        double score = 0;

        // Catégorie : +60 pts si la catégorie normalisée est dans les centres d'intérêt
        String categorie = normaliser(a.getCategorie().getNom());
        if (!interets.isEmpty() && interets.contains(categorie)) {
            score += POIDS_CATEGORIE;
        }

        // Tags : jusqu'à +25 pts proportionnellement (tags matchés / total tags) * 25
        if (a.getTags() != null && !a.getTags().isBlank()) {
            String[] tagsActivite = a.getTags().split(",");
            long correspondances = Arrays.stream(tagsActivite)
                    .map(RecommandationService::normaliser)
                    .filter(tag -> !tag.isBlank() && interets.contains(tag))
                    .count();
            if (tagsActivite.length > 0) {
                score += POIDS_TAGS * ((double) correspondances / tagsActivite.length);
            }
        }

        // Budget : +15 pts si budgetMax == null ou prix <= budgetMax
        //          bonus partiel si dépassement : max(0, 15 * (1 - (prix - budgetMax) / budgetMax))
        if (budgetMax == null || a.getPrix().compareTo(budgetMax) <= 0) {
            score += POIDS_BUDGET;
        } else {
            // dépassement : prix > budgetMax
            double prix = a.getPrix().doubleValue();
            double budget = budgetMax.doubleValue();
            double bonusPartiel = POIDS_BUDGET * Math.max(0, 1.0 - (prix - budget) / budget);
            score += bonusPartiel;
        }

        return (int) Math.min(100, Math.round(score));
    }

    private List<RecommandationDTO> raffinerAvecLlm(List<RecommandationDTO> candidates, List<String> interets,
                                                    BigDecimal budgetMax) {
        if (candidates.isEmpty()) {
            return candidates;
        }

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

        JsonNode json = ollamaClient.genererJson(prompt);

        Map<Long, RecommandationDTO> parId = candidates.stream()
                .collect(Collectors.toMap(r -> r.activite().getId(), r -> r, (x, y) -> x, LinkedHashMap::new));

        List<RecommandationDTO> resultat = new ArrayList<>();
        if (json != null && json.path("recommandations").isArray()) {
            for (JsonNode n : json.path("recommandations")) {
                long id = n.path("id").asLong(-1);
                RecommandationDTO r = parId.remove(id);
                if (r != null) {
                    String raison = n.path("raison").asText("").trim();
                    resultat.add(new RecommandationDTO(r.activite(), r.score(),
                            raison.isEmpty() ? raisonParDefaut(r) : raison));
                }
            }
        }

        // Activités non citées par le LLM (ou LLM indisponible) : on garde l'ordre du scoring
        for (RecommandationDTO r : parId.values()) {
            resultat.add(new RecommandationDTO(r.activite(), r.score(), raisonParDefaut(r)));
        }
        return resultat;
    }

    private String raisonParDefaut(RecommandationDTO r) {
        return "Correspond à vos centres d'intérêt (catégorie « " + r.activite().getCategorie().getNom()
                + " », pertinence " + r.score() + "/100).";
    }

    private static String echapper(String s) {
        return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    static String normaliser(String s) {
        if (s == null) return "";
        String sansAccents = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return sansAccents.toLowerCase().trim();
    }
}
