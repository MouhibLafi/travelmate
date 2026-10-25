package com.travelmate.accommodation.service.impl;

import com.travelmate.accommodation.config.CacheConfig;
import com.travelmate.accommodation.dto.*;
import com.travelmate.accommodation.entity.Accommodation;
import com.travelmate.accommodation.exception.InvalidRequestException;
import com.travelmate.accommodation.exception.ResourceNotFoundException;
import com.travelmate.accommodation.mapper.AccommodationMapper;
import com.travelmate.accommodation.repository.AccommodationRepository;
import com.travelmate.accommodation.service.AccommodationService;
import com.travelmate.accommodation.specification.AccommodationSpecifications;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AccommodationServiceImpl implements AccommodationService {

    static final Set<String> SORTABLE_FIELDS = Set.of("name", "pricePerNight", "rating", "stars", "createdAt", "city", "maxGuests");

    private final AccommodationRepository repository;
    private final AccommodationMapper mapper;

    @Override
    public PageResponse<AccommodationSummary> search(AccommodationSearchCriteria criteria, Pageable pageable) {
        Pageable safe = sanitize(pageable);
        return PageResponse.of(
                repository.findAll(AccommodationSpecifications.fromCriteria(criteria), safe),
                mapper::toSummary);
    }

    @Override
    public AccommodationResponse getById(Long id) {
        return mapper.toResponse(findOrThrow(id));
    }

    @Override
    @Transactional
    public AccommodationResponse create(AccommodationRequest request) {
        Accommodation saved = repository.save(mapper.toEntity(request));
        log.info("Created accommodation {} ({})", saved.getId(), saved.getName());
        return mapper.toResponse(saved);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = CacheConfig.PRICE_ANALYSIS_CACHE, allEntries = true)
    public AccommodationResponse update(Long id, AccommodationRequest request) {
        Accommodation entity = findOrThrow(id);
        mapper.updateEntity(entity, request);
        return mapper.toResponse(repository.saveAndFlush(entity));
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = CacheConfig.PRICE_ANALYSIS_CACHE, allEntries = true)
    public void delete(Long id) {
        repository.delete(findOrThrow(id));
        log.info("Deleted accommodation {}", id);
    }

    @Override
    public List<String> listCities() {
        return repository.findDistinctCities();
    }

    private Accommodation findOrThrow(Long id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Accommodation", id));
    }

    /** Only whitelisted properties can be used for sorting; a stable secondary sort on id keeps pages consistent. */
    private Pageable sanitize(Pageable pageable) {
        for (Sort.Order order : pageable.getSort()) {
            if (!SORTABLE_FIELDS.contains(order.getProperty())) {
                throw new InvalidRequestException("Cannot sort by '" + order.getProperty() + "'. Allowed: " + SORTABLE_FIELDS);
            }
        }
        Sort sort = pageable.getSort().and(Sort.by("id"));
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
    }
}
