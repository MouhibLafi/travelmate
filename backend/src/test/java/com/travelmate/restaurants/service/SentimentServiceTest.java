package com.travelmate.restaurants.service;

import com.travelmate.restaurants.entity.Sentiment;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

// Teste l'analyse locale (sans Ollama), utilisée comme secours quand Llama 3 est indisponible.
class SentimentServiceTest {

    // null : on ne teste que l'analyse locale, aucun appel à Ollama
    private final SentimentService sentimentService = new SentimentService(null);

    @Test
    void unAvisElogieuxEstPositif() {
        ResultatSentiment resultat = sentimentService.analyserLocalement(
                "Repas excellent et service impeccable, je recommande vivement.");
        assertThat(resultat.sentiment()).isEqualTo(Sentiment.POSITIF);
        assertThat(resultat.score()).isPositive();
    }

    @Test
    void unAvisNegatifEstDetecte() {
        ResultatSentiment resultat = sentimentService.analyserLocalement(
                "Plat fade et service tres lent, vraiment decevant.");
        assertThat(resultat.sentiment()).isEqualTo(Sentiment.NEGATIF);
        assertThat(resultat.score()).isNegative();
    }

    @Test
    void laNegationPasBonEstComprise() {
        ResultatSentiment resultat = sentimentService.analyserLocalement("Le plat etait pas bon du tout.");
        assertThat(resultat.sentiment()).isEqualTo(Sentiment.NEGATIF);
    }

    @Test
    void unAvisSansMotCleEstNeutre() {
        ResultatSentiment resultat = sentimentService.analyserLocalement("Restaurant correct.");
        assertThat(resultat.sentiment()).isEqualTo(Sentiment.NEUTRE);
        assertThat(resultat.score()).isZero();
    }

    @Test
    void lesAccentsNeBloquentPasLanalyse() {
        ResultatSentiment resultat = sentimentService.analyserLocalement(
                "Cuisine délicieuse et accueil très chaleureux.");
        assertThat(resultat.sentiment()).isEqualTo(Sentiment.POSITIF);
    }

    @Test
    void leSourceEstLocaleParDefaut() {
        ResultatSentiment resultat = sentimentService.analyserLocalement("Tres bon repas.");
        assertThat(resultat.source()).isEqualTo("LOCAL");
    }
}
