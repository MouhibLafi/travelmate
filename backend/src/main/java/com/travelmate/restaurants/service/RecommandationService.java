package com.travelmate.restaurants.service;

import com.travelmate.restaurants.dto.RecommandationDTO;
import com.travelmate.restaurants.entity.Restaurant;
import com.travelmate.restaurants.entity.Sentiment;
import com.travelmate.restaurants.mapper.RestaurantMapper;
import com.travelmate.restaurants.repository.AvisRepository;
import com.travelmate.restaurants.repository.RestaurantRepository;
import com.travelmate.restaurants.repository.RestaurantSpecifications;
import com.travelmate.users.entity.BudgetLevel;
import com.travelmate.users.service.UserService;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

/*
 * Recommandation = note moyenne (45 %) + part d'avis positifs (35 %) + respect du budget (20 %).
 * Les avis ont été classés POSITIF / NÉGATIF / NEUTRE par l'IA (Ollama ou analyse locale).
 * Si l'utilisateur ne donne pas de budget, on utilise le niveau de son profil (module 1).
 */
@Service
@Transactional(readOnly = true)
public class RecommandationService {

    private final RestaurantRepository restaurantRepository;
    private final AvisRepository avisRepository;
    private final UserService userService;

    public RecommandationService(RestaurantRepository restaurantRepository,
                                 AvisRepository avisRepository,
                                 UserService userService) {
        this.restaurantRepository = restaurantRepository;
        this.avisRepository = avisRepository;
        this.userService = userService;
    }

    public List<RecommandationDTO> recommander(String email, String cuisine, Double budgetMax,
                                               List<String> options, String ville, int limite) {
        Specification<Restaurant> specification = RestaurantSpecifications.avecFiltres(
                null, cuisine, ville, null, null, null, options);
        List<Restaurant> restaurants = restaurantRepository.findAll(specification);
        int nombreMax = limite <= 0 ? 5 : Math.min(limite, 20);

        Double budget = budgetEffectif(email, budgetMax);

        return restaurants.stream()
                .map(restaurant -> evaluer(restaurant, budget))
                .sorted(Comparator.comparingDouble(RecommandationDTO::score).reversed())
                .limit(nombreMax)
                .toList();
    }

    /*
     * Budget par personne déduit du profil quand le formulaire est laissé vide.
     * Ces montants sont une convention du module 4, affichés dans l'interface.
     */
    private Double budgetEffectif(String email, Double budgetMax) {
        if (budgetMax != null && budgetMax > 0) {
            return budgetMax;
        }
        BudgetLevel niveau = userService.findByEmail(email).getPreferences().getBudgetLevel();
        if (niveau == null) {
            return null;
        }
        return switch (niveau) {
            case ECONOMIQUE -> 20.0;
            case MOYEN -> 35.0;
            case LUXE -> 60.0;
        };
    }

    private RecommandationDTO evaluer(Restaurant restaurant, Double budgetMax) {
        long total = avisRepository.countByRestaurantId(restaurant.getId());
        long positifs = avisRepository.countByRestaurantIdAndSentiment(restaurant.getId(), Sentiment.POSITIF);
        long negatifs = avisRepository.countByRestaurantIdAndSentiment(restaurant.getId(), Sentiment.NEGATIF);
        long neutres = total - positifs - negatifs;

        double qualite = restaurant.getNombreAvis() == 0 ? 0.6 : restaurant.getNoteMoyenne() / 5.0;
        double satisfaction = total == 0 ? 0.5 : (positifs + 0.5 * neutres) / total;

        boolean dansBudget = true;
        double respectBudget = 1.0;
        if (budgetMax != null && budgetMax > 0) {
            if (restaurant.getPrixMoyen() <= budgetMax) {
                respectBudget = 1.0;
            } else {
                dansBudget = false;
                respectBudget = Math.max(0, 1 - (restaurant.getPrixMoyen() - budgetMax) / budgetMax);
            }
        }

        double score = Math.round(100 * (0.45 * qualite + 0.35 * satisfaction + 0.20 * respectBudget)) / 100.0;

        String avisTexte = total == 0
                ? "pas encore d'avis"
                : Math.round(100.0 * positifs / total) + " % d'avis positifs";
        String budgetTexte = budgetMax == null
                ? String.format("%.0f € par personne", restaurant.getPrixMoyen())
                : dansBudget
                    ? String.format("dans votre budget (%.0f €)", restaurant.getPrixMoyen())
                    : String.format("%.0f €, au-dessus du budget", restaurant.getPrixMoyen());

        String raison = String.format("Note %.1f/5, %s, %s",
                restaurant.getNoteMoyenne(), avisTexte, budgetTexte);

        return new RecommandationDTO(RestaurantMapper.versDto(restaurant), score, dansBudget, raison);
    }
}
