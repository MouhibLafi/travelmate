package com.travelmate.accommodation.controller;

import com.travelmate.accommodation.dto.AiStatusResponse;
import com.travelmate.accommodation.dto.PriceAnalysisResponse;
import com.travelmate.accommodation.service.PriceAnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@Validated
@RestController
@RequestMapping("/api/accommodations")
@RequiredArgsConstructor
@Tag(name = "AI price analysis", description = "Module 5 - fair price prediction and cheaper alternatives (Ollama / llama3)")
public class PriceAnalysisController {

    private final PriceAnalysisService service;

    @GetMapping("/{id}/price-analysis")
    @Operation(summary = "Fair price estimate + up to 5 cheaper similar accommodations",
            description = "Comparables -> statistical baseline -> llama3 -> validation. If the local AI is down, "
                    + "the statistical estimate is returned with aiAvailable=false. Results are cached for 10 minutes.")
    public PriceAnalysisResponse analyze(@PathVariable Long id,
                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut,
                                         @RequestParam(required = false) @Min(1) @Max(30) Integer guests) {
        return service.analyze(id, checkIn, checkOut, guests);
    }

    @GetMapping("/ai/status")
    @Operation(summary = "Is the local AI reachable? (503 if not)")
    public AiStatusResponse aiStatus() {
        return service.aiStatus();
    }
}
