package com.travelmate.accommodation.mapper;

import com.travelmate.accommodation.dto.AccommodationRequest;
import com.travelmate.accommodation.dto.AccommodationResponse;
import com.travelmate.accommodation.dto.AccommodationSummary;
import com.travelmate.accommodation.dto.BookedPeriodResponse;
import com.travelmate.accommodation.entity.Accommodation;
import com.travelmate.accommodation.entity.Amenity;
import com.travelmate.accommodation.entity.BookedPeriod;
import org.springframework.stereotype.Component;

import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/** Manual entity/DTO mapping: entities never leave the service layer. */
@Component
public class AccommodationMapper {

    public Accommodation toEntity(AccommodationRequest request) {
        Accommodation entity = new Accommodation();
        updateEntity(entity, request);
        return entity;
    }

    public void updateEntity(Accommodation entity, AccommodationRequest request) {
        entity.setName(request.name().trim());
        entity.setDescription(request.description());
        entity.setType(request.type());
        entity.setCity(request.city().trim());
        entity.setCountry(request.country().trim());
        entity.setAddress(request.address());
        entity.setStars(request.stars());
        entity.setRating(request.rating());
        entity.setPricePerNight(request.pricePerNight().setScale(2, RoundingMode.HALF_UP));
        entity.setMaxGuests(request.maxGuests());
        entity.setDestinationId(request.destinationId());

        entity.getAmenities().clear();
        if (request.amenities() != null) {
            entity.getAmenities().addAll(request.amenities());
        }
        entity.getImageUrls().clear();
        if (request.imageUrls() != null) {
            request.imageUrls().stream().map(String::trim).forEach(entity.getImageUrls()::add);
        }
    }

    public AccommodationResponse toResponse(Accommodation a) {
        return new AccommodationResponse(
                a.getId(), a.getName(), a.getDescription(), a.getType(),
                a.getCity(), a.getCountry(), a.getAddress(),
                a.getStars(), a.getRating(), a.getPricePerNight(), a.getMaxGuests(),
                a.getDestinationId(),
                copyAmenities(a.getAmenities()),
                new ArrayList<>(a.getImageUrls()),
                a.getCreatedAt(), a.getUpdatedAt());
    }

    public AccommodationSummary toSummary(Accommodation a) {
        List<String> images = a.getImageUrls();
        return new AccommodationSummary(
                a.getId(), a.getName(), a.getType(), a.getCity(), a.getCountry(),
                a.getStars(), a.getRating(), a.getPricePerNight(), a.getMaxGuests(),
                copyAmenities(a.getAmenities()),
                images.isEmpty() ? null : images.get(0));
    }

    public BookedPeriodResponse toPeriodResponse(BookedPeriod p) {
        return new BookedPeriodResponse(
                p.getId(), p.getAccommodation().getId(),
                p.getStartDate(), p.getEndDate(), p.getStatus(), p.getNote());
    }

    private static Set<Amenity> copyAmenities(Set<Amenity> amenities) {
        return amenities.isEmpty() ? EnumSet.noneOf(Amenity.class) : EnumSet.copyOf(amenities);
    }
}
