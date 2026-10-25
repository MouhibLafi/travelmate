package com.travelmate.accommodation.controller;

import com.travelmate.accommodation.dto.*;
import com.travelmate.accommodation.service.AccommodationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/accommodations")
@RequiredArgsConstructor
@Tag(name = "Accommodations", description = "Module 5 - accommodation catalog and search")
public class AccommodationController {

    private final AccommodationService service;

    @GetMapping
    @Operation(summary = "Search accommodations",
            description = "Paginated search. Sort with e.g. sort=pricePerNight,asc. "
                    + "When checkIn/checkOut are given, accommodations booked on those dates are excluded.")
    public PageResponse<AccommodationSummary> search(
            @Valid @ParameterObject AccommodationSearchCriteria criteria,
            @ParameterObject @PageableDefault(size = 12, sort = "pricePerNight", direction = Sort.Direction.ASC) Pageable pageable) {
        return service.search(criteria, pageable);
    }

    @GetMapping("/cities")
    @Operation(summary = "Distinct cities present in the catalog (for autocomplete)")
    public List<String> cities() {
        return service.listCities();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get one accommodation")
    public AccommodationResponse get(@Parameter(description = "Accommodation id") @PathVariable Long id) {
        return service.getById(id);
    }

    @PostMapping
    @Operation(summary = "Create an accommodation")
    public ResponseEntity<AccommodationResponse> create(@Valid @RequestBody AccommodationRequest request) {
        AccommodationResponse created = service.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an accommodation")
    public AccommodationResponse update(@PathVariable Long id, @Valid @RequestBody AccommodationRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete an accommodation (and its periods)")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
