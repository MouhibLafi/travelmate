package com.travelmate.accommodation.config;

import com.travelmate.accommodation.entity.Accommodation;
import com.travelmate.accommodation.entity.AccommodationType;
import com.travelmate.accommodation.entity.Amenity;
import com.travelmate.accommodation.entity.BookedPeriod;
import com.travelmate.accommodation.entity.PeriodStatus;
import com.travelmate.accommodation.repository.AccommodationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import static com.travelmate.accommodation.entity.AccommodationType.*;
import static com.travelmate.accommodation.entity.Amenity.*;

/**
 * Inserts ~42 realistic accommodations (7 cities x 6) with booked periods relative to today,
 * so search, calendar and price analysis have meaningful data. Runs only when the table is empty.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "travelmate.accommodation.seed.enabled", havingValue = "true")
public class AccommodationDataSeeder implements CommandLineRunner {

    private static final String IMG = "https://images.unsplash.com/%s?auto=format&fit=crop&w=1200&q=70";

    private static final List<String> HOTEL_IMAGES = List.of(
            "photo-1566073771259-6a8506099945", "photo-1551882547-ff40c63fe5fa", "photo-1542314831-068cd1dbfeeb",
            "photo-1520250497591-112f2f40a3f4", "photo-1582719478250-c89cae4dc85b", "photo-1571896349842-33c89424de2d",
            "photo-1564501049412-61c2a3083791", "photo-1590490360182-c33d57733427", "photo-1611892440504-42a792e24d32",
            "photo-1631049307264-da0ec9d70304", "photo-1618773928121-c32242e63f39", "photo-1445019980597-93fa8acb246c",
            "photo-1578683010236-d716f9a3f461", "photo-1540541338287-41700207dee6");

    private static final List<String> HOME_IMAGES = List.of(
            "photo-1522708323590-d24dbb6b0267", "photo-1502672260266-1c1ef2d93688", "photo-1560448204-e02f11c3d0e2",
            "photo-1555854877-bab0e564b8d5", "photo-1596394516093-501ba68a0ba6", "photo-1584132967334-10e028bd69f7",
            "photo-1595576508898-0ad5c879a061", "photo-1505693416388-ac5ce068fe85", "photo-1586375300773-8384e3e4916f",
            "photo-1600585154340-be6161a56a0c");

    private final AccommodationRepository repository;

    @Override
    @Transactional
    public void run(String... args) {
        if (repository.count() > 0) {
            log.info("Accommodation seed skipped: table already contains data");
            return;
        }
        List<Accommodation> all = new ArrayList<>();

        // ---------------- Rome ----------------
        all.add(a("Hotel Colosseo Vista", HOTEL, "Rome", "Italy", "Via Labicana 125", 4, 4.5, 185, 3,
                "Elegant rooms a short walk from the Colosseum, with a rooftop terrace overlooking the Roman Forum.",
                WIFI, BREAKFAST, AIR_CONDITIONING, RESTAURANT, GYM));
        all.add(a("Trastevere Charm Apartment", APARTMENT, "Rome", "Italy", "Via della Lungaretta 48", 3, 4.7, 135, 4,
                "Bright apartment with wooden beams in the heart of Trastevere, fully equipped kitchen and quiet courtyard view.",
                WIFI, KITCHEN, AIR_CONDITIONING, WASHING_MACHINE));
        all.add(a("Palazzo Navona Luxury Suites", HOTEL, "Rome", "Italy", "Corso del Rinascimento 70", 5, 4.8, 390, 2,
                "Five-star boutique palace near Piazza Navona with spa, fine dining and personalised concierge.",
                WIFI, BREAKFAST, AIR_CONDITIONING, SPA, RESTAURANT, GYM, AIRPORT_SHUTTLE));
        all.add(a("The Yellow Hostel Roma", HOSTEL, "Rome", "Italy", "Via Palestro 44", 2, 4.1, 38, 1,
                "Lively social hostel next to Termini station with a bar, events every night and shared kitchen.",
                WIFI, KITCHEN, AIR_CONDITIONING));
        all.add(a("Hotel Termini Comfort", HOTEL, "Rome", "Italy", "Via Marsala 20", 3, 3.9, 110, 3,
                "Practical 3-star hotel opposite Termini station, ideal for exploring Rome by metro.",
                WIFI, BREAKFAST, AIR_CONDITIONING));
        all.add(a("Hotel Vaticano Garden", HOTEL, "Rome", "Italy", "Via Candia 60", 4, 4.3, 165, 4,
                "Family-friendly 4-star hotel in Prati, five minutes from the Vatican Museums, with a garden patio.",
                WIFI, BREAKFAST, AIR_CONDITIONING, PARKING));

        // ---------------- Paris ----------------
        all.add(a("Hôtel Saint-Germain Rive Gauche", HOTEL, "Paris", "France", "12 Rue Jacob", 4, 4.6, 260, 2,
                "Classic Parisian hotel in Saint-Germain-des-Prés, steps from cafés, galleries and the Seine.",
                WIFI, BREAKFAST, AIR_CONDITIONING, RESTAURANT));
        all.add(a("Le Marais Loft", APARTMENT, "Paris", "France", "23 Rue Vieille du Temple", 3, 4.7, 210, 4,
                "Stylish loft with exposed stone walls in Le Marais, full kitchen and washing machine.",
                WIFI, KITCHEN, WASHING_MACHINE));
        all.add(a("Hôtel Montmartre Bohème", HOTEL, "Paris", "France", "8 Rue Lepic", 3, 4.2, 145, 3,
                "Cosy hotel at the foot of Sacré-Cœur with artistic decor and a hearty breakfast.",
                WIFI, BREAKFAST));
        all.add(a("Generator Paris Canal", HOSTEL, "Paris", "France", "9-11 Place du Colonel Fabien", 2, 4.0, 45, 1,
                "Design hostel near Canal Saint-Martin with a rooftop bar and private or shared rooms.",
                WIFI, RESTAURANT));
        all.add(a("Champs-Élysées Grand Palace", HOTEL, "Paris", "France", "31 Avenue George V", 5, 4.9, 620, 2,
                "Iconic palace hotel near the Champs-Élysées with Michelin-starred dining and a luxury spa.",
                WIFI, BREAKFAST, AIR_CONDITIONING, SPA, RESTAURANT, GYM, POOL, AIRPORT_SHUTTLE));
        all.add(a("Hôtel Tour Eiffel View", HOTEL, "Paris", "France", "17 Avenue de Suffren", 4, 4.4, 240, 3,
                "Modern 4-star hotel with rooms facing the Eiffel Tower and a fitness room.",
                WIFI, AIR_CONDITIONING, GYM, BREAKFAST));

        // ---------------- Barcelona ----------------
        all.add(a("Hotel Gótico Barcelona", HOTEL, "Barcelona", "Spain", "Carrer de Jaume I 14", 4, 4.4, 175, 3,
                "Contemporary hotel in the Gothic Quarter with a rooftop pool and views over the cathedral.",
                WIFI, BREAKFAST, AIR_CONDITIONING, POOL));
        all.add(a("Eixample Modernista Apartment", APARTMENT, "Barcelona", "Spain", "Carrer de Mallorca 260", 3, 4.6, 150, 5,
                "Spacious apartment in a modernist building near Casa Batlló, with balcony and full kitchen.",
                WIFI, KITCHEN, AIR_CONDITIONING, WASHING_MACHINE));
        all.add(a("Barceloneta Beach Hostel", HOSTEL, "Barcelona", "Spain", "Carrer del Baluard 58", 2, 4.0, 35, 1,
                "Laid-back hostel two minutes from the beach, perfect for surfers and backpackers.",
                WIFI, KITCHEN, BEACH_ACCESS));
        all.add(a("W Sea View Resort Barcelona", HOTEL, "Barcelona", "Spain", "Plaça de la Rosa dels Vents 1", 5, 4.7, 420, 3,
                "Sail-shaped seafront resort with infinity pool, spa and panoramic sea views.",
                WIFI, BREAKFAST, AIR_CONDITIONING, POOL, SPA, GYM, RESTAURANT, SEA_VIEW, BEACH_ACCESS));
        all.add(a("Hotel Sagrada Família", HOTEL, "Barcelona", "Spain", "Carrer de Provença 450", 3, 4.1, 120, 3,
                "Friendly 3-star hotel facing the Sagrada Família, with a terrace bar.",
                WIFI, AIR_CONDITIONING, BREAKFAST));
        all.add(a("Casa Gràcia Guesthouse", GUESTHOUSE, "Barcelona", "Spain", "Carrer de Verdi 32", 3, 4.8, 95, 2,
                "Charming family-run guesthouse in bohemian Gràcia with homemade breakfast.",
                WIFI, BREAKFAST, AIR_CONDITIONING));

        // ---------------- Istanbul ----------------
        all.add(a("Sultanahmet Palace Hotel", HOTEL, "Istanbul", "Turkey", "Akbıyık Cd. 21, Fatih", 4, 4.5, 140, 3,
                "Ottoman-style hotel near the Blue Mosque with a rooftop terrace overlooking the Bosphorus.",
                WIFI, BREAKFAST, AIR_CONDITIONING, RESTAURANT, SEA_VIEW));
        all.add(a("Bosphorus Grand Hotel", HOTEL, "Istanbul", "Turkey", "Çırağan Cd. 32, Beşiktaş", 5, 4.8, 350, 2,
                "Luxury waterfront hotel on the Bosphorus with hammam spa, infinity pool and fine dining.",
                WIFI, BREAKFAST, AIR_CONDITIONING, POOL, SPA, GYM, RESTAURANT, SEA_VIEW, AIRPORT_SHUTTLE));
        all.add(a("Galata Tower Apartment", APARTMENT, "Istanbul", "Turkey", "Galip Dede Cd. 9, Beyoğlu", 3, 4.4, 85, 4,
                "Renovated apartment next to Galata Tower, close to Istiklal Avenue's shops and restaurants.",
                WIFI, KITCHEN, AIR_CONDITIONING, WASHING_MACHINE));
        all.add(a("Cheers Hostel Istanbul", HOSTEL, "Istanbul", "Turkey", "Zeynep Sultan Cami Sk. 21", 2, 4.3, 22, 1,
                "Backpacker hostel in the old town with a rooftop bar facing Hagia Sophia.",
                WIFI, BREAKFAST));
        all.add(a("Taksim Square Hotel", HOTEL, "Istanbul", "Turkey", "Cumhuriyet Cd. 8, Beyoğlu", 4, 4.0, 115, 3,
                "Modern business hotel on Taksim Square with gym and easy metro access.",
                WIFI, BREAKFAST, AIR_CONDITIONING, GYM));
        all.add(a("Kadıköy Garden Guesthouse", GUESTHOUSE, "Istanbul", "Turkey", "Moda Cd. 77, Kadıköy", 3, 4.6, 65, 3,
                "Peaceful guesthouse on the Asian side, with a garden and Turkish breakfast.",
                WIFI, BREAKFAST, PET_FRIENDLY));

        // ---------------- Tunis ----------------
        all.add(a("Hôtel Médina Dar El Jeld", GUESTHOUSE, "Tunis", "Tunisia", "5 Rue Dar El Jeld, Médina", 4, 4.7, 120, 2,
                "Traditional dar in the heart of the Medina with a patio, hammam and refined Tunisian cuisine.",
                WIFI, BREAKFAST, AIR_CONDITIONING, RESTAURANT, SPA));
        all.add(a("Tunis Lac Business Hotel", HOTEL, "Tunis", "Tunisia", "Rue du Lac Windermere, Les Berges du Lac", 4, 4.2, 110, 3,
                "Modern hotel in Les Berges du Lac, with pool, gym and conference facilities.",
                WIFI, BREAKFAST, AIR_CONDITIONING, POOL, GYM, PARKING));
        all.add(a("Avenue Bourguiba Hotel", HOTEL, "Tunis", "Tunisia", "45 Avenue Habib Bourguiba", 3, 3.8, 70, 3,
                "Central 3-star hotel on the main avenue, close to the Medina and the cathedral.",
                WIFI, BREAKFAST, AIR_CONDITIONING));
        all.add(a("Sidi Bou Said Blue Villa", GUESTHOUSE, "Tunis", "Tunisia", "Rue Habib Thameur, Sidi Bou Said", 4, 4.9, 160, 4,
                "White-and-blue villa with sea view terrace in the famous village of Sidi Bou Said.",
                WIFI, BREAKFAST, AIR_CONDITIONING, SEA_VIEW, PARKING));
        all.add(a("La Marsa Beach Apartment", APARTMENT, "Tunis", "Tunisia", "Avenue Taieb Mhiri, La Marsa", 3, 4.3, 75, 5,
                "Family apartment 200 m from La Marsa beach, with kitchen, balcony and parking.",
                WIFI, KITCHEN, AIR_CONDITIONING, PARKING, BEACH_ACCESS, WASHING_MACHINE));
        all.add(a("Tunis Youth Hostel Médina", HOSTEL, "Tunis", "Tunisia", "25 Rue Saïda Ajoula", 1, 3.7, 18, 1,
                "Simple, budget-friendly hostel in a historic building inside the Medina.",
                WIFI));

        // ---------------- Hammamet ----------------
        all.add(a("Hammamet Royal Beach Resort", HOTEL, "Hammamet", "Tunisia", "Zone Touristique Yasmine Hammamet", 5, 4.6, 190, 4,
                "All-inclusive beachfront resort with several pools, spa, kids club and private beach.",
                WIFI, BREAKFAST, AIR_CONDITIONING, POOL, SPA, GYM, RESTAURANT, BEACH_ACCESS, SEA_VIEW, PARKING));
        all.add(a("Yasmine Marina Hotel", HOTEL, "Hammamet", "Tunisia", "Marina Yasmine Hammamet", 4, 4.3, 120, 3,
                "Hotel facing the marina with outdoor pool and a short walk to the beach.",
                WIFI, BREAKFAST, AIR_CONDITIONING, POOL, RESTAURANT, PARKING));
        all.add(a("Médina Hammamet Hotel", HOTEL, "Hammamet", "Tunisia", "Avenue de la République", 3, 3.9, 75, 3,
                "Affordable hotel near the old Medina and Hammamet's sandy beach.",
                WIFI, BREAKFAST, AIR_CONDITIONING, POOL));
        all.add(a("Sea Breeze Apartment Hammamet", APARTMENT, "Hammamet", "Tunisia", "Rue des Orangers, Hammamet Nord", 3, 4.4, 70, 6,
                "Large apartment with sea-view terrace, kitchen and access to a shared pool.",
                WIFI, KITCHEN, AIR_CONDITIONING, POOL, SEA_VIEW, PARKING));
        all.add(a("Dar Hammamet Guesthouse", GUESTHOUSE, "Hammamet", "Tunisia", "Route Touristique, Hammamet Sud", 3, 4.7, 85, 2,
                "Tranquil guesthouse surrounded by jasmine gardens, with homemade breakfast.",
                WIFI, BREAKFAST, AIR_CONDITIONING, PET_FRIENDLY));
        all.add(a("Hammamet Palace & Spa", HOTEL, "Hammamet", "Tunisia", "Boulevard du 7 Novembre", 4, 4.5, 150, 3,
                "Seafront 4-star hotel with thalasso spa, pools and direct beach access.",
                WIFI, BREAKFAST, AIR_CONDITIONING, POOL, SPA, BEACH_ACCESS, RESTAURANT));

        // ---------------- Djerba ----------------
        all.add(a("Djerba Paradise Resort", HOTEL, "Djerba", "Tunisia", "Zone Touristique Midoun", 5, 4.6, 175, 4,
                "Luxury resort on Sidi Mahrez beach with golf access, spa, pools and many restaurants.",
                WIFI, BREAKFAST, AIR_CONDITIONING, POOL, SPA, GYM, RESTAURANT, BEACH_ACCESS, SEA_VIEW, AIRPORT_SHUTTLE));
        all.add(a("Houmt Souk Heritage Hotel", HOTEL, "Djerba", "Tunisia", "Place Hedi Chaker, Houmt Souk", 3, 4.2, 65, 3,
                "Former caravanserai turned hotel, in the old town of Houmt Souk.",
                WIFI, BREAKFAST, AIR_CONDITIONING));
        all.add(a("Menzel Djerba Guesthouse", GUESTHOUSE, "Djerba", "Tunisia", "Route de Midoun, Erriadh", 3, 4.8, 90, 4,
                "Authentic Djerbian menzel with patio, pool and traditional breakfast.",
                WIFI, BREAKFAST, POOL, AIR_CONDITIONING, PARKING));
        all.add(a("Djerba Beach Hotel", HOTEL, "Djerba", "Tunisia", "Plage Sidi Mahrez", 4, 4.1, 115, 3,
                "Beachfront 4-star hotel with large pool and animation programme.",
                WIFI, BREAKFAST, AIR_CONDITIONING, POOL, BEACH_ACCESS, RESTAURANT));
        all.add(a("Midoun Family Apartment", APARTMENT, "Djerba", "Tunisia", "Avenue Mohamed Badra, Midoun", 3, 4.3, 60, 6,
                "Spacious family apartment with kitchen, 5 minutes by car from the beaches.",
                WIFI, KITCHEN, AIR_CONDITIONING, PARKING, WASHING_MACHINE));
        all.add(a("Djerba Lagoon Hotel", HOTEL, "Djerba", "Tunisia", "Aghir, Zone Touristique", 4, 4.4, 135, 3,
                "Quiet lagoon-side hotel with spa and private beach.",
                WIFI, BREAKFAST, AIR_CONDITIONING, POOL, SPA, BEACH_ACCESS, SEA_VIEW));

        LocalDate today = LocalDate.now();
        for (int i = 0; i < all.size(); i++) {
            Accommodation acc = all.get(i);
            acc.setImageUrls(images(acc.getType(), i));
            if (i % 3 == 0) {
                acc.addPeriod(booked(today.plusDays(3 + i % 5), today.plusDays(8 + i % 5), "Demo booking"));
            }
            if (i % 4 == 1) {
                acc.addPeriod(booked(today.plusDays(14), today.plusDays(21), "Group reservation"));
            }
            if (i % 5 == 2) {
                acc.addPeriod(booked(today.minusDays(2), today.plusDays(2), "Current stay"));
                acc.addPeriod(booked(today.plusDays(30), today.plusDays(37), "Demo booking"));
            }
        }
        repository.saveAll(all);
        log.info("Seeded {} accommodations", all.size());
    }

    private static Accommodation a(String name, AccommodationType type, String city, String country, String address,
                                   int stars, double rating, int price, int maxGuests, String description,
                                   Amenity... amenities) {
        return Accommodation.builder()
                .name(name).type(type).city(city).country(country).address(address)
                .stars(stars).rating(rating).pricePerNight(BigDecimal.valueOf(price).setScale(2))
                .maxGuests(maxGuests).description(description)
                .amenities(EnumSet.copyOf(List.of(amenities)))
                .build();
    }

    private static BookedPeriod booked(LocalDate start, LocalDate end, String note) {
        return BookedPeriod.builder().startDate(start).endDate(end).status(PeriodStatus.BOOKED).note(note).build();
    }

    private static List<String> images(AccommodationType type, int index) {
        List<String> pool = (type == HOTEL || type == HOSTEL) ? HOTEL_IMAGES : HOME_IMAGES;
        List<String> urls = new ArrayList<>();
        for (int k = 0; k < 3; k++) {
            urls.add(IMG.formatted(pool.get((index * 3 + k) % pool.size())));
        }
        return urls;
    }
}
