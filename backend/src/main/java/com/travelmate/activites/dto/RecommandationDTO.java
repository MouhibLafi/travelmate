package com.travelmate.activites.dto;

import com.travelmate.activites.model.Activite;

/**
 * @param activite activité recommandée
 * @param score    score de pertinence de 0 à 100 (filtrage par contenu)
 * @param raison   explication courte (générée par Llama 3 ou message de repli)
 */
public record RecommandationDTO(Activite activite, int score, String raison) {
}
