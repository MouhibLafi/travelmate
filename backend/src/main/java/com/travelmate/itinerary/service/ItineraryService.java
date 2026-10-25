package com.travelmate.itinerary.service;

import com.travelmate.exception.ApiException;
import com.travelmate.itinerary.catalog.CatalogItem;
import com.travelmate.itinerary.catalog.CatalogProvider;
import com.travelmate.itinerary.dto.ItemRequest;
import com.travelmate.itinerary.dto.ItineraryDto;
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
import com.travelmate.users.entity.Interest;
import com.travelmate.users.event.UserDeletedEvent;
import com.travelmate.users.service.UserService;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/*
 * Itineraires de l'utilisateur CONNECTE (son email vient du jeton JWT, comme dans le module 2).
 * Un utilisateur ne peut lire ou modifier que SES itineraires.
 */
@Service
public class ItineraryService {

    private final ItineraryRepository itineraryRepository;
    private final ItineraryDayRepository dayRepository;
    private final ItineraryItemRepository itemRepository;
    private final TripRepository tripRepository;       // module 2
    private final UserService userService;             // module 1
    private final CatalogProvider catalogProvider;     // modules 3, 4, 5 (catalogue de demonstration pour l'instant)
    private final AiItineraryService aiItineraryService;

    public ItineraryService(ItineraryRepository itineraryRepository, ItineraryDayRepository dayRepository,
                            ItineraryItemRepository itemRepository, TripRepository tripRepository,
                            UserService userService, CatalogProvider catalogProvider,
                            AiItineraryService aiItineraryService) {
        this.itineraryRepository = itineraryRepository;
        this.dayRepository = dayRepository;
        this.itemRepository = itemRepository;
        this.tripRepository = tripRepository;
        this.userService = userService;
        this.catalogProvider = catalogProvider;
        this.aiItineraryService = aiItineraryService;
    }

    /*
     * Genere (ou regenere) l'itineraire d'un voyage avec l'IA.
     * Pas de @Transactional ici : l'appel a Ollama peut durer une minute.
     */
    public ItineraryDto generate(String email, Long tripId) {
        Trip trip = findMyTrip(email, tripId);
        TripInfo info = TripInfo.from(trip, interests(email, trip));
        ItineraryPlan plan = aiItineraryService.plan(info,
                catalogProvider.getActivities(info.destination()),
                catalogProvider.getRestaurants(info.destination()),
                catalogProvider.getHotels(info.destination()));

        itineraryRepository.deleteAll(itineraryRepository.findByTripId(tripId));
        Itinerary itinerary = toEntity(info, plan);
        itinerary.setUser(trip.getUser());
        return ItineraryDto.from(itineraryRepository.save(itinerary));
    }

    // Les centres d'interet du voyage, sinon ceux des preferences de l'utilisateur (module 1)
    private List<Interest> interests(String email, Trip trip) {
        if (trip.getInterests() != null && !trip.getInterests().isEmpty()) {
            return new ArrayList<>(trip.getInterests());
        }
        var preferred = userService.getPreferences(email).interests();
        return preferred == null ? List.of() : new ArrayList<>(preferred);
    }

    private Itinerary toEntity(TripInfo trip, ItineraryPlan plan) {
        Itinerary it = new Itinerary();
        it.setTripId(trip.id());
        it.setDestination(trip.destination());
        it.setStartDate(trip.startDate());
        it.setEndDate(trip.endDate());
        it.setBudget(trip.budget());
        it.setGeneratedByAi(plan.aiUsed());

        for (ItineraryPlan.Day planDay : plan.days()) {
            ItineraryDay day = new ItineraryDay(planDay.dayNumber(),
                    trip.startDate().plusDays(planDay.dayNumber() - 1L), planDay.title());
            // L'hebergement est pose le premier jour, pour toutes les nuits
            if (planDay.dayNumber() == 1 && plan.hotel() != null) {
                ItineraryItem hotel = fromCatalog(plan.hotel(), LocalTime.of(8, 0), null, trip.numberOfNights());
                hotel.setNotes("Arrivée, " + trip.numberOfNights() + " nuit(s)");
                day.addItem(hotel);
            }
            for (ItineraryPlan.Slot slot : planDay.slots()) {
                ItineraryItem item = fromCatalog(slot.item(), slot.start(), slot.end(), 1);
                item.setNotes(slot.note());
                day.addItem(item);
            }
            it.addDay(day);
        }
        it.recalculateTotal();
        return it;
    }

    private ItineraryItem fromCatalog(CatalogItem c, LocalTime start, LocalTime end, int quantity) {
        ItineraryItem item = new ItineraryItem();
        item.setType(c.type());
        item.setRefId(c.id());
        item.setName(c.name());
        item.setCategory(c.category());
        item.setStartTime(start);
        item.setEndTime(end);
        item.setPricing(c.price(), quantity);
        return item;
    }

    // Mes itineraires. Ceux dont le voyage a ete supprime dans le module 2 sont supprimes au passage.
    @Transactional
    public List<ItineraryDto> findMine(String email) {
        List<ItineraryDto> result = new ArrayList<>();
        for (Itinerary it : itineraryRepository.findByUserEmailOrderByCreatedAtDesc(email)) {
            if (tripRepository.existsById(it.getTripId())) {
                result.add(ItineraryDto.from(it));
            } else {
                itineraryRepository.delete(it);
            }
        }
        return result;
    }

    @Transactional(readOnly = true)
    public ItineraryDto getById(String email, Long id) {
        return ItineraryDto.from(findMyItinerary(email, id));
    }

    @Transactional
    public void delete(String email, Long id) {
        itineraryRepository.delete(findMyItinerary(email, id));
    }

    // Activites, restaurants ou hebergements de la destination, pour ajouter un element a la main
    @Transactional(readOnly = true)
    public List<CatalogItem> getCatalog(String email, Long itineraryId, ItemType type) {
        return catalogProvider.getByType(type, findMyItinerary(email, itineraryId).getDestination());
    }

    @Transactional
    public ItineraryDto addItem(String email, Long itineraryId, Long dayId, ItemRequest request) {
        Itinerary it = findMyItinerary(email, itineraryId);
        ItineraryDay day = dayRepository.findById(dayId)
                .filter(d -> d.getItinerary().getId().equals(it.getId()))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Journée introuvable"));
        ItineraryItem item = new ItineraryItem();
        apply(item, request, it.getDestination());
        day.addItem(item);
        return saveWithTotal(it);
    }

    @Transactional
    public ItineraryDto updateItem(String email, Long itemId, ItemRequest request) {
        ItineraryItem item = findMyItem(email, itemId);
        Itinerary it = item.getDay().getItinerary();
        apply(item, request, it.getDestination());
        return saveWithTotal(it);
    }

    @Transactional
    public ItineraryDto deleteItem(String email, Long itemId) {
        ItineraryItem item = findMyItem(email, itemId);
        ItineraryDay day = item.getDay();
        day.getItems().remove(item);
        return saveWithTotal(day.getItinerary());
    }

    // Appele automatiquement quand le module 1 supprime un utilisateur : on supprime ses itineraires
    @EventListener
    @Transactional
    public void onUserDeleted(UserDeletedEvent event) {
        itineraryRepository.deleteAll(itineraryRepository.findByUserId(event.userId()));
    }

    // Copie la demande dans l'element ; avec un refId du catalogue, le nom et le prix vides sont repris du catalogue
    private void apply(ItineraryItem item, ItemRequest r, String destination) {
        CatalogItem c = r.refId() == null ? null
                : catalogProvider.find(r.type(), r.refId(), destination).orElse(null);
        String name = r.name() != null && !r.name().isBlank() ? r.name() : c != null ? c.name() : null;
        if (name == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Le nom est obligatoire pour un élément hors catalogue");
        }
        double unitPrice = r.unitPrice() != null ? r.unitPrice() : c != null ? c.price() : 0;
        item.setType(r.type());
        item.setRefId(c != null ? c.id() : null);
        item.setName(name);
        item.setCategory(r.category() != null ? r.category() : c != null ? c.category() : null);
        item.setStartTime(r.startTime());
        item.setEndTime(r.endTime());
        item.setPricing(unitPrice, r.quantity() != null ? r.quantity() : 1);
        item.setNotes(r.notes());
    }

    private ItineraryDto saveWithTotal(Itinerary it) {
        it.recalculateTotal();
        return ItineraryDto.from(itineraryRepository.save(it));
    }

    // ---------- Un utilisateur ne peut voir / modifier que SES donnees ----------

    Itinerary findMyItinerary(String email, Long id) {
        Itinerary it = itineraryRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Itinéraire introuvable"));
        checkOwner(email, it);
        return it;
    }

    private ItineraryItem findMyItem(String email, Long id) {
        ItineraryItem item = itemRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Élément introuvable"));
        checkOwner(email, item.getDay().getItinerary());
        return item;
    }

    private Trip findMyTrip(String email, Long tripId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Voyage introuvable"));
        if (!trip.getUser().getEmail().equals(email)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Ce voyage ne vous appartient pas");
        }
        return trip;
    }

    private void checkOwner(String email, Itinerary it) {
        if (!it.getUser().getEmail().equals(email)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Cet itinéraire ne vous appartient pas");
        }
    }
}
