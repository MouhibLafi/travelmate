package com.travelmate.activites.config;

import com.travelmate.activites.model.Activite;
import com.travelmate.activites.model.Categorie;
import com.travelmate.activites.model.Destination;
import com.travelmate.activites.repository.ActiviteRepository;
import com.travelmate.activites.repository.CategorieRepository;
import com.travelmate.activites.repository.DestinationRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/** Insère des données de démonstration au premier démarrage (si la base est vide). */
@Component
public class DataSeeder implements CommandLineRunner {

    private final CategorieRepository categorieRepository;
    private final DestinationRepository destinationRepository;
    private final ActiviteRepository activiteRepository;

    public DataSeeder(CategorieRepository categorieRepository, DestinationRepository destinationRepository,
                      ActiviteRepository activiteRepository) {
        this.categorieRepository = categorieRepository;
        this.destinationRepository = destinationRepository;
        this.activiteRepository = activiteRepository;
    }

    @Override
    public void run(String... args) {
        if (destinationRepository.count() > 0) {
            return;
        }

        Map<String, Categorie> cat = new HashMap<>();
        for (String nom : new String[]{"Culture", "Nature", "Gastronomie", "Aventure", "Détente", "Histoire", "Sport", "Shopping"}) {
            cat.put(nom, categorieRepository.save(new Categorie(nom)));
        }

        Destination rome = destinationRepository.save(new Destination("Rome", "Italie",
                "La ville éternelle : monuments antiques, places baroques et cuisine généreuse.", null, "histoire,culture,gastronomie"));
        Destination paris = destinationRepository.save(new Destination("Paris", "France",
                "Musées de renommée mondiale, balades au bord de la Seine et gastronomie.", null, "culture,gastronomie,shopping"));
        Destination tunis = destinationRepository.save(new Destination("Tunis", "Tunisie",
                "Médina classée, site de Carthage, café de Sidi Bou Saïd face à la Méditerranée.", null, "histoire,culture,détente"));
        Destination lisbonne = destinationRepository.save(new Destination("Lisbonne", "Portugal",
                "Collines colorées, tramways, fado et pastéis de nata.", null, "culture,gastronomie,détente"));
        Destination interlaken = destinationRepository.save(new Destination("Interlaken", "Suisse",
                "Entre deux lacs et face aux Alpes : paradis des sports et de la nature.", null, "nature,aventure,sport"));

        // Rome
        add(rome, cat.get("Histoire"), "Visite du Colisée et du Forum romain", "Billet combiné avec accès à l'arène et au Palatin.", "18", 180, "antiquité,monument,archéologie");
        add(rome, cat.get("Culture"), "Musées du Vatican et Chapelle Sixtine", "Parcours à travers les collections papales.", "25", 210, "musée,art,peinture");
        add(rome, cat.get("Gastronomie"), "Cours de cuisine : pâtes fraîches", "Atelier pour apprendre à préparer des pâtes à l'italienne.", "65", 180, "cuisine,atelier,pâtes");
        add(rome, cat.get("Détente"), "Balade à la Villa Borghese", "Promenade dans le grand parc de la ville, location de vélo possible.", "0", 120, "parc,balade,gratuit");

        // Paris
        add(paris, cat.get("Culture"), "Musée du Louvre", "Des chefs-d'œuvre de l'Antiquité au XIXe siècle.", "22", 240, "musée,art,peinture");
        add(paris, cat.get("Histoire"), "Tour Eiffel et Champ-de-Mars", "Montée en ascenseur et vue panoramique sur la capitale.", "29", 120, "monument,panorama");
        add(paris, cat.get("Gastronomie"), "Dégustation de fromages et vins", "Dégustation guidée chez un caviste du Marais.", "55", 120, "vin,fromage,dégustation");
        add(paris, cat.get("Shopping"), "Marché aux puces de Saint-Ouen", "Antiquités, vêtements vintage et objets de collection.", "0", 150, "marché,vintage,gratuit");

        // Tunis
        add(tunis, cat.get("Histoire"), "Site archéologique de Carthage", "Thermes d'Antonin, tophet et colline de Byrsa.", "12", 150, "antiquité,archéologie,monument");
        add(tunis, cat.get("Culture"), "Médina de Tunis et souks", "Visite de la médina classée à l'UNESCO, mosquée Zitouna.", "0", 180, "médina,artisanat,unesco");
        add(tunis, cat.get("Détente"), "Café et balade à Sidi Bou Saïd", "Ruelles bleues et blanches, vue sur le golfe.", "5", 120, "village,panorama,balade");
        add(tunis, cat.get("Culture"), "Musée national du Bardo", "L'une des plus riches collections de mosaïques romaines au monde.", "13", 150, "musée,mosaïque,art");

        // Lisbonne
        add(lisbonne, cat.get("Culture"), "Tramway 28 et quartier de l'Alfama", "Balade dans le plus ancien quartier de la ville.", "3", 120, "quartier,tramway,balade");
        add(lisbonne, cat.get("Gastronomie"), "Tour des pastéis de nata", "Dégustations dans les pâtisseries historiques.", "35", 150, "pâtisserie,dégustation");
        add(lisbonne, cat.get("Culture"), "Soirée fado", "Dîner-spectacle de musique traditionnelle portugaise.", "40", 120, "musique,spectacle,traditionnel");
        add(lisbonne, cat.get("Nature"), "Excursion à Sintra", "Palais de Pena, forêt et quinta da Regaleira.", "30", 300, "palais,forêt,randonnée");

        // Interlaken
        add(interlaken, cat.get("Aventure"), "Parapente au-dessus des Alpes", "Vol biplace avec moniteur.", "180", 90, "parapente,adrénaline,vol");
        add(interlaken, cat.get("Nature"), "Randonnée au lac de Brienz", "Sentier panoramique entre lac et montagne.", "0", 240, "randonnée,lac,montagne");
        add(interlaken, cat.get("Sport"), "Rafting sur la rivière Lütschine", "Descente en eaux vives avec guide.", "95", 180, "rafting,eau,adrénaline");
        add(interlaken, cat.get("Détente"), "Croisière sur le lac de Thoune", "Tour en bateau avec vue sur les montagnes.", "40", 150, "bateau,lac,panorama");
    }

    private void add(Destination d, Categorie c, String nom, String description, String prix, int duree, String tags) {
        Activite a = new Activite();
        a.setDestination(d);
        a.setCategorie(c);
        a.setNom(nom);
        a.setDescription(description);
        a.setPrix(new BigDecimal(prix));
        a.setDureeMinutes(duree);
        a.setTags(tags);
        activiteRepository.save(a);
    }
}
