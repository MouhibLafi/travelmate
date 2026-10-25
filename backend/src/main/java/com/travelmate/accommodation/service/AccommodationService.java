package com.travelmate.accommodation.service;

import com.travelmate.accommodation.dto.*;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface AccommodationService {

    PageResponse<AccommodationSummary> search(AccommodationSearchCriteria criteria, Pageable pageable);

    AccommodationResponse getById(Long id);

    AccommodationResponse create(AccommodationRequest request);

    AccommodationResponse update(Long id, AccommodationRequest request);

    void delete(Long id);

    List<String> listCities();
}
