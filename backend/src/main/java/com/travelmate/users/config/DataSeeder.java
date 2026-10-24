package com.travelmate.users.config;

import com.travelmate.users.entity.BudgetLevel;
import com.travelmate.users.entity.Interest;
import com.travelmate.users.entity.Role;
import com.travelmate.users.entity.User;
import com.travelmate.users.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;

import static com.travelmate.users.entity.BudgetLevel.*;
import static com.travelmate.users.entity.Interest.*;

/*
 * Base de demonstration : 20 utilisateurs (2 ADMIN, 18 VOYAGEUR dont 2 desactives).
 * S'execute au demarrage et cree seulement les comptes qui n'existent pas encore (par email).
 *
 *   admin@travelmate.tn  / admin123      (ADMIN)
 *   tous les autres      / password123
 *
 * Les voyages de demonstration sont crees par le module 2 (trips/config/TripDataSeeder).
 */
@Component
@Order(1)   // avant les donnees des autres modules, qui ont besoin des utilisateurs
public class DataSeeder implements CommandLineRunner {

    // Un utilisateur de demonstration : description = la phrase donnee a l'IA (null = preferences pas encore remplies)
    record DemoUser(String firstName, String lastName, String email, String phone, Role role, boolean active,
                    String description, List<Interest> interests, BudgetLevel budget) {
    }

    static final List<DemoUser> USERS = List.of(
            new DemoUser("Admin", "TravelMate", "admin@travelmate.tn", null, Role.ADMIN, true,
                    null, List.of(), null),
            new DemoUser("Mouhib", "Lafi", "mouhib@travelmate.tn", "+216 22 410 815", Role.ADMIN, true,
                    "Je rêve du Japon : temples, jardins et cuisine raffinée, je peux mettre le prix.",
                    List.of(CULTURE, GASTRONOMIE), LUXE),
            new DemoUser("Yasmine", "Trabelsi", "yasmine@travelmate.tn", "+216 25 301 774", Role.VOYAGEUR, true,
                    "Je suis étudiante, j'adore la plage et la street food.",
                    List.of(PLAGE, GASTRONOMIE), ECONOMIQUE),
            new DemoUser("Karim", "Ben Salah", "karim@travelmate.tn", "+216 98 112 430", Role.VOYAGEUR, true,
                    "Passionné de randonnée et de photo en montagne, avec un budget raisonnable.",
                    List.of(NATURE, AVENTURE), MOYEN),
            new DemoUser("Amira", "Jlassi", "amira@travelmate.tn", "+216 52 678 019", Role.VOYAGEUR, true,
                    "Musées, vieilles médinas et cafés culturels, en hôtel confortable.",
                    List.of(CULTURE, HISTOIRE, GASTRONOMIE), MOYEN),
            new DemoUser("Mehdi", "Bouazizi", "mehdi@travelmate.tn", null, Role.VOYAGEUR, true,
                    "Je veux faire du surf la journée et sortir le soir.",
                    List.of(SPORT, VIE_NOCTURNE, PLAGE), MOYEN),
            new DemoUser("Ines", "Hammami", "ines@travelmate.tn", "+216 20 555 901", Role.VOYAGEUR, true,
                    "Spa, hammam et repos total dans des hôtels haut de gamme.",
                    List.of(DETENTE, GASTRONOMIE), LUXE),
            new DemoUser("Youssef", "Mansour", "youssef@travelmate.tn", "+216 29 870 342", Role.VOYAGEUR, true,
                    "Plongée, escalade et sensations fortes, avec un petit budget.",
                    List.of(AVENTURE, SPORT), ECONOMIQUE),
            new DemoUser("Nour", "Ayari", "nour@travelmate.tn", null, Role.VOYAGEUR, true,
                    "J'aime chiner dans les souks et goûter la cuisine traditionnelle.",
                    List.of(SHOPPING, GASTRONOMIE, CULTURE), MOYEN),
            new DemoUser("Hamza", "Chaabane", "hamza@travelmate.tn", "+216 55 204 668", Role.VOYAGEUR, true,
                    "Ski en hiver, vélo en été, et je ne compte pas.",
                    List.of(SPORT, NATURE), LUXE),
            new DemoUser("Rania", "Khelifi", "rania@travelmate.tn", "+216 23 987 120", Role.VOYAGEUR, true,
                    "Sites romains, histoire ancienne et désert du Sud, pas cher.",
                    List.of(HISTOIRE, NATURE, CULTURE), ECONOMIQUE),
            new DemoUser("Oussama", "Ferchichi", "oussama@travelmate.tn", null, Role.VOYAGEUR, true,
                    null, List.of(), null),   // nouvel inscrit : preferences pas encore remplies
            new DemoUser("Mariem", "Zouari", "mariem@travelmate.tn", "+216 97 330 456", Role.VOYAGEUR, true,
                    "Plage, détente et couchers de soleil, budget moyen.",
                    List.of(PLAGE, DETENTE), MOYEN),
            new DemoUser("Aymen", "Sassi", "aymen@travelmate.tn", null, Role.VOYAGEUR, true,
                    "Concerts, bars et vie nocturne dans les grandes villes.",
                    List.of(VIE_NOCTURNE, CULTURE, SHOPPING), MOYEN),
            new DemoUser("Sarra", "Ben Amor", "sarra@travelmate.tn", "+216 50 118 902", Role.VOYAGEUR, true,
                    "Boutiques de créateurs, grands restaurants et hôtels de luxe.",
                    List.of(SHOPPING, GASTRONOMIE, DETENTE), LUXE),
            new DemoUser("Fares", "Dridi", "fares@travelmate.tn", "+216 26 743 015", Role.VOYAGEUR, true,
                    "Camping dans le désert et randonnée, le moins cher possible.",
                    List.of(NATURE, AVENTURE), ECONOMIQUE),
            new DemoUser("Lina", "Mejri", "lina@travelmate.tn", null, Role.VOYAGEUR, true,
                    "Art contemporain, théâtre et musées européens.",
                    List.of(CULTURE), MOYEN),
            new DemoUser("Walid", "Toumi", "walid@travelmate.tn", "+216 92 605 287", Role.VOYAGEUR, true,
                    "Châteaux, histoire et gastronomie française, avec du confort.",
                    List.of(HISTOIRE, GASTRONOMIE, CULTURE), LUXE),
            new DemoUser("Bilel", "Haddad", "bilel@travelmate.tn", "+216 24 019 863", Role.VOYAGEUR, false,
                    "Football et matchs à l'étranger, petit budget.",
                    List.of(SPORT, VIE_NOCTURNE), ECONOMIQUE),   // compte desactive par l'admin
            new DemoUser("Emna", "Rekik", "emna@travelmate.tn", null, Role.VOYAGEUR, false,
                    null, List.of(), null)                       // compte desactive, sans preferences
    );

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        for (DemoUser demo : USERS) {
            if (!userRepository.existsByEmail(demo.email())) {
                userRepository.save(toUser(demo));
            }
        }
    }

    private User toUser(DemoUser demo) {
        User user = new User();
        user.setFirstName(demo.firstName());
        user.setLastName(demo.lastName());
        user.setEmail(demo.email());
        user.setPhone(demo.phone());
        user.setRole(demo.role());
        user.setActive(demo.active());
        user.setPassword(passwordEncoder.encode(demo.email().equals("admin@travelmate.tn") ? "admin123" : "password123"));

        if (demo.description() != null) {
            user.getPreferences().setDescription(demo.description());
            user.getPreferences().setInterests(new HashSet<>(demo.interests()));
            user.getPreferences().setBudgetLevel(demo.budget());
            user.getPreferences().setAiGenerated(true);   // preferences issues de la suggestion de l'IA
        }
        return user;
    }
}
