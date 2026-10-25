package com.travelmate.itinerary.service;

import com.travelmate.exception.ApiException;
import com.travelmate.itinerary.catalog.CatalogProvider;
import com.travelmate.itinerary.dto.ItemRequest;
import com.travelmate.itinerary.dto.TripInfo;
import com.travelmate.itinerary.entity.ItemType;
import com.travelmate.itinerary.entity.Itinerary;
import com.travelmate.itinerary.entity.ItineraryDay;
import com.travelmate.itinerary.entity.ItineraryItem;
import com.travelmate.itinerary.repository.ItineraryDayRepository;
import com.travelmate.itinerary.repository.ItineraryItemRepository;
import com.travelmate.itinerary.repository.ItineraryRepository;
import com.travelmate.trips.entity.Trip;
import com.travelmate.trips.repository.TripRepository;
import com.travelmate.users.dto.PreferencesDto;
import com.travelmate.users.entity.BudgetLevel;
import com.travelmate.users.entity.Interest;
import com.travelmate.users.entity.User;
import com.travelmate.users.event.UserDeletedEvent;
import com.travelmate.users.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

// Integration avec les modules 1 et 2 : proprietaire, voyages, preferences, suppression d'un utilisateur
class ItineraryServiceTest {

    private static final LocalDate DAY_1 = LocalDate.of(2026, 11, 10);

    private final User yasmine = user(1L, "yasmine@travelmate.tn");
    private final User karim = user(2L, "karim@travelmate.tn");

    private ItineraryRepository itineraries;
    private ItineraryItemRepository items;
    private TripRepository trips;
    private UserService userService;
    private AiItineraryService ai;
    private ItineraryService service;

    @BeforeEach
    void setUp() {
        itineraries = mock(ItineraryRepository.class);
        items = mock(ItineraryItemRepository.class);
        trips = mock(TripRepository.class);
        userService = mock(UserService.class);
        ai = mock(AiItineraryService.class);
        CatalogProvider catalog = mock(CatalogProvider.class);
        service = new ItineraryService(itineraries, mock(ItineraryDayRepository.class), items, trips, userService,
                catalog, ai);
    }

    @Test
    void litSonPropreItineraire() {
        stored(10L, yasmine);

        assertThat(service.getById(yasmine.getEmail(), 10L).id()).isEqualTo(10L);
    }

    @Test
    void refuseLItineraireDUnAutre() {
        stored(20L, karim);

        assertThatThrownBy(() -> service.getById(yasmine.getEmail(), 20L))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getStatus()).isEqualTo(HttpStatus.FORBIDDEN));
        assertThatThrownBy(() -> service.delete(yasmine.getEmail(), 20L)).isInstanceOf(ApiException.class);
        verify(itineraries, never()).delete(any());
    }

    @Test
    void itineraireIntrouvable() {
        assertThatThrownBy(() -> service.getById(yasmine.getEmail(), 99L))
                .isInstanceOf(ApiException.class)
                .hasMessage("Itinéraire introuvable");
    }

    @Test
    void refuseDeModifierUnElementDUnAutre() {
        Itinerary theirs = stored(20L, karim);
        ItineraryItem item = new ItineraryItem();
        item.setId(5L);
        theirs.getDays().get(0).addItem(item);
        when(items.findById(5L)).thenReturn(Optional.of(item));

        ItemRequest request = new ItemRequest(ItemType.TRANSPORT, null, "Taxi", null, null, null, 30.0, 1, null);
        assertThatThrownBy(() -> service.updateItem(yasmine.getEmail(), 5L, request)).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> service.deleteItem(yasmine.getEmail(), 5L)).isInstanceOf(ApiException.class);
    }

    @Test
    void refuseDeGenererPourLeVoyageDUnAutre() {
        when(trips.findById(7L)).thenReturn(Optional.of(trip(7L, karim, Set.of())));

        assertThatThrownBy(() -> service.generate(yasmine.getEmail(), 7L)).isInstanceOf(ApiException.class);
        verifyNoInteractions(ai);
    }

    @Test
    void utiliseLesPreferencesSiLeVoyageNaPasDeCentresDInteret() {
        when(trips.findById(7L)).thenReturn(Optional.of(trip(7L, yasmine, Set.of())));
        when(userService.getPreferences(yasmine.getEmail()))
                .thenReturn(new PreferencesDto("", Set.of(Interest.PLAGE), BudgetLevel.ECONOMIQUE, false));
        when(ai.plan(any(), anyList(), anyList(), anyList())).thenReturn(new ItineraryPlan(null, List.of(), false));
        when(itineraries.save(any())).thenAnswer(call -> call.getArgument(0));

        service.generate(yasmine.getEmail(), 7L);

        ArgumentCaptor<TripInfo> info = ArgumentCaptor.forClass(TripInfo.class);
        verify(ai).plan(info.capture(), anyList(), anyList(), anyList());
        assertThat(info.getValue().interests()).containsExactly(Interest.PLAGE);
    }

    @Test
    void supprimeLItineraireDontLeVoyageNExistePlus() {
        Itinerary kept = stored(10L, yasmine);
        Itinerary orphan = stored(11L, yasmine);
        orphan.setTripId(99L);
        when(itineraries.findByUserEmailOrderByCreatedAtDesc(yasmine.getEmail())).thenReturn(List.of(kept, orphan));
        when(trips.existsById(1L)).thenReturn(true);
        when(trips.existsById(99L)).thenReturn(false);

        assertThat(service.findMine(yasmine.getEmail())).extracting(d -> d.id()).containsExactly(10L);
        verify(itineraries).delete(orphan);
    }

    @Test
    void supprimeLesItinerairesQuandLUtilisateurEstSupprime() {
        List<Itinerary> hers = List.of(stored(10L, yasmine));
        when(itineraries.findByUserId(1L)).thenReturn(hers);

        service.onUserDeleted(new UserDeletedEvent(1L));

        verify(itineraries).deleteAll(hers);
    }

    // ---------- Outils ----------

    private Itinerary stored(long id, User owner) {
        Itinerary it = new Itinerary();
        it.setId(id);
        it.setUser(owner);
        it.setTripId(1L);
        it.setDestination("Rome");
        it.setStartDate(DAY_1);
        it.setEndDate(DAY_1);
        it.setBudget(500);
        it.addDay(new ItineraryDay(1, DAY_1, "Jour 1"));
        when(itineraries.findById(id)).thenReturn(Optional.of(it));
        return it;
    }

    private static Trip trip(long id, User owner, Set<Interest> interests) {
        Trip trip = new Trip();
        trip.setId(id);
        trip.setUser(owner);
        trip.setDestination("Djerba");
        trip.setStartDate(DAY_1);
        trip.setEndDate(DAY_1.plusDays(2));
        trip.setBudget(350.0);
        trip.setInterests(interests);
        return trip;
    }

    private static User user(long id, String email) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        return user;
    }
}
