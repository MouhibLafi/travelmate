package com.travelmate.restaurants.dto;

import java.util.List;

public record MetaFiltresDTO(
        List<String> cuisines,
        List<String> villes,
        List<String> optionsAlimentaires,
        double prixMinimum,
        double prixMaximum
) {
}
