package com.travelmate.restaurants.service;

import com.travelmate.restaurants.dto.AnalyseAvisDTO;
import com.travelmate.restaurants.dto.AvisDTO;
import com.travelmate.restaurants.dto.AvisRequest;
import com.travelmate.restaurants.dto.ReanalyseDTO;
import com.travelmate.restaurants.entity.Avis;
import com.travelmate.restaurants.entity.Restaurant;
import com.travelmate.restaurants.entity.Sentiment;
import com.travelmate.restaurants.mapper.RestaurantMapper;
import com.travelmate.restaurants.repository.AvisRepository;
import com.travelmate.restaurants.repository.RestaurantRepository;
import com.travelmate.users.entity.User;
import com.travelmate.users.event.UserDeletedEvent;
import com.travelmate.users.service.UserService;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AvisService {

    private final AvisRepository avisRepository;
    private final RestaurantRepository restaurantRepository;
    private final RestaurantService restaurantService;
    private final SentimentService sentimentService;
    private final OllamaStatutService ollamaStatutService;
    private final UserService userService;   // module 1 : retrouve l'auteur connecté

    public AvisService(AvisRepository avisRepository,
                       RestaurantRepository restaurantRepository,
                       RestaurantService restaurantService,
                       SentimentService sentimentService,
                       OllamaStatutService ollamaStatutService,
                       UserService userService) {
        this.avisRepository = avisRepository;
        this.restaurantRepository = restaurantRepository;
        this.restaurantService = restaurantService;
        this.sentimentService = sentimentService;
        this.ollamaStatutService = ollamaStatutService;
        this.userService = userService;
    }

    @Transactional(readOnly = true)
    public List<AvisDTO> lister(Long restaurantId) {
        restaurantService.trouverEntite(restaurantId);
        return avisRepository.findByRestaurantIdOrderByDateCreationDesc(restaurantId).stream()
                .map(RestaurantMapper::versDto)
                .toList();
    }

    /** L'email vient du jeton JWT : un utilisateur ne peut pas écrire au nom d'un autre. */
    @Transactional
    public AvisDTO ajouter(Long restaurantId, String email, AvisRequest requete) {
        Restaurant restaurant = restaurantService.trouverEntite(restaurantId);
        User auteur = userService.findByEmail(email);

        ResultatSentiment resultat = sentimentService.analyser(requete.commentaire());

        Avis avis = new Avis(restaurant, auteur, requete.note(), requete.commentaire().trim());
        avis.setSentiment(resultat.sentiment());
        avis.setScoreSentiment(resultat.score());
        avis.setSourceSentiment(resultat.source());
        avisRepository.save(avis);

        mettreAJourNote(restaurant);
        return RestaurantMapper.versDto(avis);
    }

    @Transactional(readOnly = true)
    public AnalyseAvisDTO analyser(Long restaurantId) {
        Restaurant restaurant = restaurantService.trouverEntite(restaurantId);
        List<Avis> avis = avisRepository.findByRestaurantIdOrderByDateCreationDesc(restaurantId);
        long total = avis.size();
        long positifs = avis.stream().filter(a -> a.getSentiment() == Sentiment.POSITIF).count();
        long negatifs = avis.stream().filter(a -> a.getSentiment() == Sentiment.NEGATIF).count();
        long neutres = total - positifs - negatifs;

        List<String> pointsForts = avis.stream()
                .filter(a -> a.getSentiment() == Sentiment.POSITIF)
                .sorted(Comparator.comparingDouble(Avis::getScoreSentiment).reversed())
                .limit(3)
                .map(Avis::getCommentaire)
                .toList();

        List<String> pointsFaibles = avis.stream()
                .filter(a -> a.getSentiment() == Sentiment.NEGATIF)
                .sorted(Comparator.comparingDouble(Avis::getScoreSentiment))
                .limit(3)
                .map(Avis::getCommentaire)
                .toList();

        return new AnalyseAvisDTO(
                restaurantId,
                (int) total,
                pourcentage(positifs, total),
                pourcentage(negatifs, total),
                pourcentage(neutres, total),
                restaurant.getNoteMoyenne(),
                pointsForts,
                pointsFaibles
        );
    }

    /** Bouton "Réanalyser avec l'IA" : relance Llama 3 sur tous les avis déjà enregistrés. */
    @Transactional
    public ReanalyseDTO reanalyserTous() {
        if (!ollamaStatutService.estDisponible()) {
            return new ReanalyseDTO(0, 0,
                    "Ollama est indisponible. Démarrez Ollama (ollama serve) puis réessayez.");
        }

        List<Avis> avis = avisRepository.findAll();
        int modifies = 0;
        for (Avis unAvis : avis) {
            var resultat = sentimentService.analyserAvecOllama(unAvis.getCommentaire());
            if (resultat.isEmpty()) {
                continue;
            }
            ResultatSentiment sentiment = resultat.get();
            if (unAvis.getSentiment() != sentiment.sentiment()
                    || unAvis.getScoreSentiment() != sentiment.score()) {
                modifies++;
            }
            unAvis.setSentiment(sentiment.sentiment());
            unAvis.setScoreSentiment(sentiment.score());
            unAvis.setSourceSentiment(sentiment.source());
        }
        return new ReanalyseDTO(avis.size(), modifies,
                "Analyse terminée avec " + ollamaStatutService.getModele()
                        + " : " + modifies + " avis mis à jour.");
    }

    /*
     * Appelé par le module 1 avant la suppression d'un compte.
     * On supprime ses avis, puis on recalcule la note des restaurants concernés.
     */
    @EventListener
    @Transactional
    public void onUserDeleted(UserDeletedEvent event) {
        List<Avis> avis = avisRepository.findByAuteurId(event.userId());
        if (avis.isEmpty()) {
            return;
        }
        Set<Long> restaurantsTouches = avis.stream()
                .map(avisUnitaire -> avisUnitaire.getRestaurant().getId())
                .collect(Collectors.toSet());

        avisRepository.deleteByAuteurId(event.userId());

        for (Long restaurantId : restaurantsTouches) {
            restaurantRepository.findById(restaurantId).ifPresent(this::mettreAJourNote);
        }
    }

    private void mettreAJourNote(Restaurant restaurant) {
        Double moyenne = avisRepository.moyenneNotes(restaurant.getId());
        long nombre = avisRepository.countByRestaurantId(restaurant.getId());
        restaurant.setNoteMoyenne(moyenne == null ? 0 : Math.round(moyenne * 10.0) / 10.0);
        restaurant.setNombreAvis((int) nombre);
        restaurantRepository.save(restaurant);
    }

    private double pourcentage(long valeur, long total) {
        if (total == 0) {
            return 0;
        }
        return Math.round(valeur * 1000.0 / total) / 10.0;
    }
}
