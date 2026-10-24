package com.travelmate.trips.service;

import com.travelmate.exception.ApiException;
import com.travelmate.trips.dto.TripRequest;
import com.travelmate.trips.dto.TripResponse;
import com.travelmate.trips.entity.Trip;
import com.travelmate.trips.repository.TripRepository;
import com.travelmate.users.event.UserDeletedEvent;
import com.travelmate.users.service.UserService;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;

@Service
public class TripService {

    private final TripRepository tripRepository;
    private final UserService userService;   // service du module 1

    public TripService(TripRepository tripRepository, UserService userService) {
        this.tripRepository = tripRepository;
        this.userService = userService;
    }

    public List<TripResponse> findMyTrips(String email) {
        return tripRepository.findByUserEmailOrderByStartDateAsc(email).stream()
                .map(TripResponse::from)
                .toList();
    }

    public TripResponse create(String email, TripRequest request) {
        Trip trip = new Trip();
        trip.setUser(userService.findByEmail(email));   // on reutilise le module 1
        fill(trip, request);
        return TripResponse.from(tripRepository.save(trip));
    }

    public TripResponse update(String email, Long id, TripRequest request) {
        Trip trip = findMyTrip(email, id);
        fill(trip, request);
        return TripResponse.from(tripRepository.save(trip));
    }

    public void delete(String email, Long id) {
        tripRepository.delete(findMyTrip(email, id));
    }

    // Appele automatiquement quand le module 1 supprime un utilisateur : on supprime ses voyages
    @EventListener
    public void onUserDeleted(UserDeletedEvent event) {
        tripRepository.deleteAll(tripRepository.findByUserId(event.userId()));
    }

    // Un utilisateur ne peut voir / modifier que SES voyages
    private Trip findMyTrip(String email, Long id) {
        Trip trip = tripRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Voyage introuvable"));
        if (!trip.getUser().getEmail().equals(email)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Ce voyage ne vous appartient pas");
        }
        return trip;
    }

    private void fill(Trip trip, TripRequest request) {
        if (request.endDate().isBefore(request.startDate())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "La date de fin doit être après la date de début");
        }
        trip.setDestination(request.destination());
        trip.setStartDate(request.startDate());
        trip.setEndDate(request.endDate());
        trip.setBudget(request.budget());
        trip.setInterests(request.interests() == null ? new HashSet<>() : new HashSet<>(request.interests()));
        trip.setBudgetLevel(request.budgetLevel());
    }
}
