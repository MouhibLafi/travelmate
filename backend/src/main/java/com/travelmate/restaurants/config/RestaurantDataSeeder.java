package com.travelmate.restaurants.config;

import com.travelmate.restaurants.entity.Avis;
import com.travelmate.restaurants.entity.Favori;
import com.travelmate.restaurants.entity.Restaurant;
import com.travelmate.restaurants.repository.AvisRepository;
import com.travelmate.restaurants.repository.FavoriRepository;
import com.travelmate.restaurants.repository.RestaurantRepository;
import com.travelmate.restaurants.service.ResultatSentiment;
import com.travelmate.restaurants.service.SentimentService;
import com.travelmate.users.entity.User;
import com.travelmate.users.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/*
 * Données de démonstration du module 4 : 12 restaurants, 48 avis et quelques favoris.
 * S'exécute APRÈS le DataSeeder du module 1 (les avis ont besoin des utilisateurs)
 * et seulement si la table des restaurants est vide.
 */
@Component
@Order(3)
public class RestaurantDataSeeder implements CommandLineRunner {

    // Auteurs possibles : des comptes actifs créés par le module 1
    private static final List<String> AUTEURS = List.of(
            "yasmine@travelmate.tn", "karim@travelmate.tn", "amira@travelmate.tn",
            "mehdi@travelmate.tn", "ines@travelmate.tn", "youssef@travelmate.tn",
            "nour@travelmate.tn", "hamza@travelmate.tn", "rania@travelmate.tn",
            "oussama@travelmate.tn", "mariem@travelmate.tn", "aymen@travelmate.tn",
            "sarra@travelmate.tn", "fares@travelmate.tn", "lina@travelmate.tn",
            "walid@travelmate.tn", "mouhib@travelmate.tn", "admin@travelmate.tn"
    );

    private final RestaurantRepository restaurantRepository;
    private final AvisRepository avisRepository;
    private final FavoriRepository favoriRepository;
    private final UserRepository userRepository;
    private final SentimentService sentimentService;

    private int compteurAvis = 0;

    public RestaurantDataSeeder(RestaurantRepository restaurantRepository,
                                AvisRepository avisRepository,
                                FavoriRepository favoriRepository,
                                UserRepository userRepository,
                                SentimentService sentimentService) {
        this.restaurantRepository = restaurantRepository;
        this.avisRepository = avisRepository;
        this.favoriRepository = favoriRepository;
        this.userRepository = userRepository;
        this.sentimentService = sentimentService;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (restaurantRepository.count() > 0) {
            return;
        }

        Restaurant darElJeld = creerRestaurant("Dar El Jeld", "Tunisienne",
                "Cuisine tunisienne raffinée dans un palais de la Médina, spécialités de saison.",
                "5 rue Dar El Jeld, Médina", "Tunis", 45,
                "https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?auto=format&fit=crop&w=800&q=60",
                options("Halal"));
        Restaurant chezSlah = creerRestaurant("Chez Slah", "Tunisienne",
                "Poissons grillés et spécialités du sud dans une ambiance familiale.",
                "Route de la plage, Houmt Souk", "Djerba", 15,
                "https://images.unsplash.com/photo-1555396273-367ea4eb4db5?auto=format&fit=crop&w=800&q=60",
                options("Halal"));
        Restaurant trattoriaBella = creerRestaurant("Trattoria Bella", "Italienne",
                "Pâtes fraîches, pizzas au feu de bois et desserts italiens.",
                "12 avenue de la Liberté", "Tunis", 22,
                "https://images.unsplash.com/photo-1565299624946-b28f40a0ae38?auto=format&fit=crop&w=800&q=60",
                options("Végétarien"));
        Restaurant sakuraSushi = creerRestaurant("Sakura Sushi", "Japonaise",
                "Sushis et makis préparés à la minute avec des produits frais.",
                "Boulevard du 14 janvier", "Sousse", 30,
                "https://images.unsplash.com/photo-1579871494447-9811cf80d66c?auto=format&fit=crop&w=800&q=60",
                options("Halal"));
        Restaurant petitParis = creerRestaurant("Le Petit Paris", "Française",
                "Bistrot français : plats traditionnels et desserts maison.",
                "Rue de la Marine", "Carthage", 40,
                "https://images.unsplash.com/photo-1414235077428-338989a2e8c0?auto=format&fit=crop&w=800&q=60",
                options("Végétarien"));
        Restaurant beyrouthExpress = creerRestaurant("Beyrouth Express", "Libanaise",
                "Mezzés, falafels et chawarma généreux à prix doux.",
                "30 rue de Marseille", "Tunis", 18,
                "https://images.unsplash.com/photo-1544148103-0773bf10d330?auto=format&fit=crop&w=800&q=60",
                options("Halal", "Végétarien"));
        Restaurant curryHouse = creerRestaurant("Curry House", "Indienne",
                "Currys parfumés, naans et biryani préparés par un chef indien.",
                "Avenue Hédi Chaker", "Sousse", 20,
                "https://images.unsplash.com/photo-1514933651103-005eec06c04b?auto=format&fit=crop&w=800&q=60",
                options("Halal", "Végétarien"));
        Restaurant elRancho = creerRestaurant("El Rancho", "Mexicaine",
                "Tacos, burritos et guacamole préparés maison.",
                "Zone touristique Yasmine", "Hammamet", 25,
                "https://images.unsplash.com/photo-1559339352-11d035aa65de?auto=format&fit=crop&w=800&q=60",
                options("Végétarien"));
        Restaurant tableVerte = creerRestaurant("La Table Verte", "Végétarienne",
                "Cuisine végétale de saison avec des produits bio locaux.",
                "18 rue du Safranier", "La Marsa", 28,
                "https://images.unsplash.com/photo-1540189549336-e6e99c3679fe?auto=format&fit=crop&w=800&q=60",
                options("Végétarien", "Vegan", "Sans gluten"));
        Restaurant oceanBlue = creerRestaurant("Ocean Blue", "Méditerranéenne",
                "Poissons frais et fruits de mer sur une terrasse face à la mer.",
                "Corniche, Yasmine Hammamet", "Hammamet", 32,
                "https://images.unsplash.com/photo-1552566626-52f8b828add9?auto=format&fit=crop&w=800&q=60",
                options("Halal", "Sans gluten"));
        Restaurant greenBowl = creerRestaurant("Green Bowl", "Healthy",
                "Bowls, salades et jus pressés à emporter pour un repas léger.",
                "7 rue du Lac Léman", "Tunis", 14,
                "https://images.unsplash.com/photo-1590846406792-0adc7f938f1d?auto=format&fit=crop&w=800&q=60",
                options("Végétarien", "Vegan"));
        Restaurant leGolfe = creerRestaurant("Le Golfe", "Méditerranéenne",
                "Terrasse face au golfe et spécialités de la mer.",
                "Corniche de La Marsa", "La Marsa", 35,
                "https://images.unsplash.com/photo-1466978913421-dad2ebd01d17?auto=format&fit=crop&w=800&q=60",
                options("Halal"));

        ajouterAvis(darElJeld, 5, "Repas excellent, service impeccable et cadre magnifique.", 12);
        ajouterAvis(darElJeld, 4, "Très bon couscous, un peu cher mais ça vaut le détour.", 9);
        ajouterAvis(darElJeld, 5, "Cuisine authentique et accueil chaleureux, je recommande.", 6);
        ajouterAvis(darElJeld, 2, "Plat froid et service très lent, déçu pour ce prix.", 3);

        ajouterAvis(chezSlah, 5, "Poisson frais et prix abordable, parfait pour le déjeuner.", 15);
        ajouterAvis(chezSlah, 4, "Copieux et savoureux, cadre simple mais agréable.", 11);
        ajouterAvis(chezSlah, 3, "Correct sans plus, la salle est bruyante.", 7);
        ajouterAvis(chezSlah, 5, "Le meilleur poisson grillé de Djerba, foncez !", 2);

        ajouterAvis(trattoriaBella, 5, "Pâtes fraîches délicieuses et personnel adorable.", 14);
        ajouterAvis(trattoriaBella, 4, "Bonne pizza et ambiance conviviale.", 10);
        ajouterAvis(trattoriaBella, 2, "Pizza fade et trop grasse, service long.", 8);
        ajouterAvis(trattoriaBella, 5, "Rapport qualité prix excellent, on reviendra.", 1);

        ajouterAvis(sakuraSushi, 5, "Sushis très frais et présentation soignée.", 13);
        ajouterAvis(sakuraSushi, 4, "Bon sushi, service un peu long le week-end.", 9);
        ajouterAvis(sakuraSushi, 2, "Poisson pas frais, j'ai été malade.", 5);
        ajouterAvis(sakuraSushi, 5, "Meilleur japonais de Sousse, service rapide.", 2);

        ajouterAvis(petitParis, 5, "Cuisine française parfaite, dessert maison succulent.", 16);
        ajouterAvis(petitParis, 4, "Très bon steak, addition un peu élevée.", 12);
        ajouterAvis(petitParis, 2, "Plat insipide et serveur impoli.", 6);
        ajouterAvis(petitParis, 5, "Cadre élégant et repas délicieux.", 3);

        ajouterAvis(beyrouthExpress, 5, "Mezzés généreux et savoureux, prix imbattable.", 11);
        ajouterAvis(beyrouthExpress, 4, "Très bon falafel, service rapide.", 8);
        ajouterAvis(beyrouthExpress, 3, "Moyen, manque de goût.", 5);
        ajouterAvis(beyrouthExpress, 5, "Adore ce restaurant, toujours frais et copieux.", 1);

        ajouterAvis(curryHouse, 5, "Curry authentique et parfaitement épicé, délicieux.", 10);
        ajouterAvis(curryHouse, 4, "Bon biryani, salle un peu bruyante.", 7);
        ajouterAvis(curryHouse, 2, "Plat gras et fade, déçu.", 4);
        ajouterAvis(curryHouse, 5, "Excellent rapport qualité prix, je recommande.", 1);

        ajouterAvis(elRancho, 5, "Tacos délicieux et guacamole frais.", 9);
        ajouterAvis(elRancho, 4, "Bon burrito et ambiance agréable.", 6);
        ajouterAvis(elRancho, 2, "Service lent et plats froids.", 3);
        ajouterAvis(elRancho, 5, "Super soirée, cuisine généreuse et savoureuse.", 1);

        ajouterAvis(tableVerte, 5, "Cuisine végétale inventive et savoureuse, parfait.", 12);
        ajouterAvis(tableVerte, 4, "Très bon bowl, un peu cher.", 8);
        ajouterAvis(tableVerte, 3, "Correct mais petites portions.", 4);
        ajouterAvis(tableVerte, 5, "Produits frais et accueil chaleureux, je recommande.", 1);

        ajouterAvis(oceanBlue, 5, "Poisson frais et vue magnifique, parfait.", 13);
        ajouterAvis(oceanBlue, 4, "Très bons fruits de mer, service un peu lent.", 9);
        ajouterAvis(oceanBlue, 2, "Trop cher pour la qualité, décevant.", 5);
        ajouterAvis(oceanBlue, 5, "Cadre superbe et repas délicieux.", 2);

        ajouterAvis(greenBowl, 5, "Bol frais et savoureux, parfait pour le midi.", 8);
        ajouterAvis(greenBowl, 4, "Bon et abordable, service rapide.", 6);
        ajouterAvis(greenBowl, 3, "Correct, rien d'exceptionnel.", 3);
        ajouterAvis(greenBowl, 5, "Excellent rapport qualité prix, adresse top.", 1);

        ajouterAvis(leGolfe, 5, "Cadre magnifique et poisson délicieux.", 10);
        ajouterAvis(leGolfe, 4, "Très bon repas, addition un peu cher.", 6);
        ajouterAvis(leGolfe, 2, "Attente très longue et plat moyen.", 4);
        ajouterAvis(leGolfe, 5, "Service chaleureux et cuisine parfaite.", 1);

        recalculerNotes();

        // Favoris du compte étudiant de démonstration
        ajouterFavori("yasmine@travelmate.tn", darElJeld);
        ajouterFavori("yasmine@travelmate.tn", tableVerte);
        ajouterFavori("mouhib@travelmate.tn", leGolfe);
    }

    private Restaurant creerRestaurant(String nom, String cuisine, String description, String adresse,
                                       String ville, double prixMoyen, String imageUrl, Set<String> options) {
        return restaurantRepository.save(new Restaurant(nom, cuisine, description, adresse, ville,
                prixMoyen, imageUrl, options));
    }

    private Set<String> options(String... valeurs) {
        return new LinkedHashSet<>(List.of(valeurs));
    }

    // Les auteurs tournent dans la liste : chaque avis a un vrai utilisateur et un email unique par restaurant.
    private void ajouterAvis(Restaurant restaurant, int note, String commentaire, int joursAvant) {
        String email = AUTEURS.get(compteurAvis % AUTEURS.size());
        compteurAvis++;

        User auteur = userRepository.findByEmail(email).orElse(null);
        if (auteur == null) {
            return;
        }

        Avis avis = new Avis(restaurant, auteur, note, commentaire);
        ResultatSentiment sentiment = sentimentService.analyserLocalement(commentaire);
        avis.setSentiment(sentiment.sentiment());
        avis.setScoreSentiment(sentiment.score());
        avis.setSourceSentiment(sentiment.source());
        avis.setDateCreation(LocalDateTime.now().minusDays(joursAvant));
        avisRepository.save(avis);
    }

    private void ajouterFavori(String email, Restaurant restaurant) {
        userRepository.findByEmail(email).ifPresent(utilisateur ->
                favoriRepository.save(new Favori(utilisateur, restaurant)));
    }

    private void recalculerNotes() {
        for (Restaurant restaurant : restaurantRepository.findAll()) {
            Double moyenne = avisRepository.moyenneNotes(restaurant.getId());
            long nombre = avisRepository.countByRestaurantId(restaurant.getId());
            restaurant.setNoteMoyenne(moyenne == null ? 0 : Math.round(moyenne * 10.0) / 10.0);
            restaurant.setNombreAvis((int) nombre);
            restaurantRepository.save(restaurant);
        }
    }
}
