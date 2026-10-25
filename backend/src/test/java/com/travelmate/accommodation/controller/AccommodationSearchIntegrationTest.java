package com.travelmate.accommodation.controller;

import com.travelmate.accommodation.ai.OllamaClient;
import com.travelmate.accommodation.entity.Accommodation;
import com.travelmate.accommodation.entity.AccommodationType;
import com.travelmate.accommodation.entity.Amenity;
import com.travelmate.accommodation.entity.BookedPeriod;
import com.travelmate.accommodation.entity.PeriodStatus;
import com.travelmate.accommodation.repository.AccommodationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Full stack (MVC + JPA Specifications + H2) test of GET /api/accommodations. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("h2")
@TestPropertySource(properties = "travelmate.accommodation.seed.enabled=false")
@WithMockUser   // integration : les routes /api/** demandent un utilisateur connecte (JWT du module 1)
class AccommodationSearchIntegrationTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private AccommodationRepository repository;
    @MockBean
    private OllamaClient ollamaClient;

    private final LocalDate in = LocalDate.now().plusDays(10);

    @BeforeEach
    void setUp() {
        repository.deleteAll();
        Accommodation booked = acc("Rome Booked Hotel", AccommodationType.HOTEL, "Rome", 4, 4.5, 180, 2, Amenity.WIFI, Amenity.POOL);
        booked.addPeriod(BookedPeriod.builder().startDate(in).endDate(in.plusDays(3)).status(PeriodStatus.BOOKED).build());
        repository.saveAll(List.of(
                booked,
                acc("Rome Pool Hotel", AccommodationType.HOTEL, "Rome", 4, 4.2, 150, 3, Amenity.WIFI, Amenity.POOL),
                acc("Rome Budget Hostel", AccommodationType.HOSTEL, "Rome", 2, 4.0, 30, 1, Amenity.WIFI),
                acc("Rome Family Flat", AccommodationType.APARTMENT, "Rome", 3, 4.7, 120, 6, Amenity.WIFI, Amenity.KITCHEN),
                acc("Paris Palace", AccommodationType.HOTEL, "Paris", 5, 4.9, 600, 2, Amenity.WIFI, Amenity.POOL, Amenity.SPA)));
    }

    @Test
    void filtersByCityTypePriceAndSorts() throws Exception {
        mvc.perform(get("/api/accommodations")
                        .param("city", "rome").param("type", "HOTEL").param("maxPrice", "200")
                        .param("sort", "pricePerNight,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[*].name", contains("Rome Pool Hotel", "Rome Booked Hotel")));
    }

    @Test
    void filtersByAmenitiesStarsRatingAndGuests() throws Exception {
        mvc.perform(get("/api/accommodations").param("amenities", "WIFI,POOL").param("minStars", "4"))
                .andExpect(jsonPath("$.totalElements").value(3));
        mvc.perform(get("/api/accommodations").param("minRating", "4.6"))
                .andExpect(jsonPath("$.content[*].name", containsInAnyOrder("Rome Family Flat", "Paris Palace")));
        mvc.perform(get("/api/accommodations").param("guests", "5"))
                .andExpect(jsonPath("$.content[*].name", contains("Rome Family Flat")));
    }

    @Test
    void excludesAccommodationsBookedOnRequestedDates() throws Exception {
        mvc.perform(get("/api/accommodations").param("city", "Rome")
                        .param("checkIn", in.plusDays(1).toString()).param("checkOut", in.plusDays(2).toString()))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.content[*].name", not(hasItem("Rome Booked Hotel"))));
        // check-out day of the booking is free again
        mvc.perform(get("/api/accommodations").param("city", "Rome")
                        .param("checkIn", in.plusDays(3).toString()).param("checkOut", in.plusDays(5).toString()))
                .andExpect(jsonPath("$.totalElements").value(4));
    }

    @Test
    void paginates() throws Exception {
        mvc.perform(get("/api/accommodations").param("size", "2").param("page", "1").param("sort", "pricePerNight,desc"))
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(jsonPath("$.content[0].name").value("Rome Pool Hotel"));
    }

    @Test
    void rejectsInvalidParameters() throws Exception {
        mvc.perform(get("/api/accommodations").param("sort", "password"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Cannot sort")));
        mvc.perform(get("/api/accommodations").param("minStars", "9"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.minStars").exists());
        mvc.perform(get("/api/accommodations").param("type", "CASTLE"))
                .andExpect(status().isBadRequest());
    }

    private static Accommodation acc(String name, AccommodationType type, String city, int stars, double rating,
                                     int price, int guests, Amenity... amenities) {
        return Accommodation.builder().name(name).type(type).city(city).country(city.equals("Paris") ? "France" : "Italy")
                .stars(stars).rating(rating).pricePerNight(BigDecimal.valueOf(price)).maxGuests(guests)
                .amenities(EnumSet.copyOf(List.of(amenities))).build();
    }
}
