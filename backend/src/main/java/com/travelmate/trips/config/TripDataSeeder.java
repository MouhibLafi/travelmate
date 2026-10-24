package com.travelmate.trips.config;

import com.travelmate.trips.entity.Trip;
import com.travelmate.trips.repository.TripRepository;
import com.travelmate.users.entity.BudgetLevel;
import com.travelmate.users.entity.Interest;
import com.travelmate.users.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;

import static com.travelmate.users.entity.BudgetLevel.*;
import static com.travelmate.users.entity.Interest.*;

/*
 * Voyages de demonstration du module 2 (22 voyages pour 16 utilisateurs).
 * Les dates sont calculees a partir d'aujourd'hui : il y a toujours des voyages a venir et des voyages termines.
 * S'execute apres le DataSeeder du module 1, et seulement si la table des voyages est vide.
 */
@Component
@Order(2)
public class TripDataSeeder implements CommandLineRunner {

    // startInDays : debut dans N jours (negatif = voyage deja passe) ; niveau = budget / jour (regle du module 2)
    record DemoTrip(String email, String destination, int startInDays, int days, double budget,
                    BudgetLevel level, List<Interest> interests) {
    }

    static final List<DemoTrip> TRIPS = List.of(
            new DemoTrip("admin@travelmate.tn", "Kyoto, Japon", 200, 10, 2500, LUXE, List.of(CULTURE, GASTRONOMIE)),
            new DemoTrip("yasmine@travelmate.tn", "Djerba", 40, 6, 350, ECONOMIQUE, List.of(PLAGE, GASTRONOMIE)),
            new DemoTrip("yasmine@travelmate.tn", "Hammamet", -60, 3, 150, ECONOMIQUE, List.of(PLAGE)),
            new DemoTrip("karim@travelmate.tn", "Aïn Draham", 25, 4, 400, MOYEN, List.of(NATURE, AVENTURE)),
            new DemoTrip("karim@travelmate.tn", "Chamonix, France", 120, 7, 1400, MOYEN, List.of(NATURE, SPORT)),
            new DemoTrip("amira@travelmate.tn", "Rome, Italie", 30, 5, 800, MOYEN, List.of(CULTURE, HISTOIRE)),
            new DemoTrip("amira@travelmate.tn", "Istanbul, Turquie", -90, 4, 600, MOYEN, List.of(HISTOIRE, SHOPPING, GASTRONOMIE)),
            new DemoTrip("mehdi@travelmate.tn", "Ibiza, Espagne", 70, 5, 1100, LUXE, List.of(VIE_NOCTURNE, PLAGE)),
            new DemoTrip("ines@travelmate.tn", "Maldives", 150, 8, 5200, LUXE, List.of(DETENTE, PLAGE)),
            new DemoTrip("ines@travelmate.tn", "Tozeur", -30, 3, 900, LUXE, List.of(DETENTE)),
            new DemoTrip("youssef@travelmate.tn", "Tabarka", 15, 3, 180, ECONOMIQUE, List.of(AVENTURE, SPORT)),
            new DemoTrip("nour@travelmate.tn", "Marrakech, Maroc", 55, 7, 600, MOYEN, List.of(SHOPPING, CULTURE)),
            new DemoTrip("hamza@travelmate.tn", "Zermatt, Suisse", 100, 6, 3000, LUXE, List.of(SPORT, NATURE)),
            new DemoTrip("rania@travelmate.tn", "Douz", 20, 4, 250, ECONOMIQUE, List.of(NATURE, HISTOIRE)),
            new DemoTrip("rania@travelmate.tn", "Carthage", -15, 1, 40, ECONOMIQUE, List.of(HISTOIRE)),
            new DemoTrip("mariem@travelmate.tn", "Mahdia", 45, 5, 500, MOYEN, List.of(PLAGE, DETENTE)),
            new DemoTrip("aymen@travelmate.tn", "Berlin, Allemagne", 60, 4, 700, MOYEN, List.of(VIE_NOCTURNE, CULTURE)),
            new DemoTrip("sarra@travelmate.tn", "Paris, France", 35, 4, 2400, LUXE, List.of(SHOPPING, GASTRONOMIE)),
            new DemoTrip("sarra@travelmate.tn", "Dubaï", -45, 5, 3500, LUXE, List.of(SHOPPING, DETENTE)),
            new DemoTrip("fares@travelmate.tn", "Ksar Ghilane", 10, 3, 200, ECONOMIQUE, List.of(NATURE, AVENTURE)),
            new DemoTrip("lina@travelmate.tn", "Barcelone, Espagne", 80, 5, 750, MOYEN, List.of(CULTURE)),
            new DemoTrip("walid@travelmate.tn", "Lyon, France", 90, 6, 1500, LUXE, List.of(GASTRONOMIE, HISTOIRE))
    );

    private final TripRepository tripRepository;
    private final UserRepository userRepository;

    public TripDataSeeder(TripRepository tripRepository, UserRepository userRepository) {
        this.tripRepository = tripRepository;
        this.userRepository = userRepository;
    }

    @Override
    public void run(String... args) {
        if (tripRepository.count() > 0) {
            return;
        }
        LocalDate today = LocalDate.now();
        for (DemoTrip demo : TRIPS) {
            userRepository.findByEmail(demo.email()).ifPresent(user -> {
                Trip trip = new Trip();
                trip.setUser(user);
                trip.setDestination(demo.destination());
                trip.setStartDate(today.plusDays(demo.startInDays()));
                trip.setEndDate(today.plusDays(demo.startInDays() + demo.days() - 1));   // jour de depart compris
                trip.setBudget(demo.budget());
                trip.setBudgetLevel(demo.level());
                trip.setInterests(new HashSet<>(demo.interests()));
                tripRepository.save(trip);
            });
        }
    }
}
