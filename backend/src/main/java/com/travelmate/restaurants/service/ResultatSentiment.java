package com.travelmate.restaurants.service;

import com.travelmate.restaurants.entity.Sentiment;

public record ResultatSentiment(
        Sentiment sentiment,
        double score,
        String source,
        String justification
) {
}
