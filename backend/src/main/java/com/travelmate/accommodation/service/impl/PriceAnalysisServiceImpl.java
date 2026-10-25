package com.travelmate.accommodation.service.impl;

import com.travelmate.accommodation.ai.*;
import com.travelmate.accommodation.config.CacheConfig;
import com.travelmate.accommodation.dto.*;
import com.travelmate.accommodation.entity.AccommodationType;
import com.travelmate.accommodation.entity.Amenity;
import com.travelmate.accommodation.exception.AiUnavailableException;
import com.travelmate.accommodation.exception.InvalidRequestException;
import com.travelmate.accommodation.service.PriceAnalysisService;
import com.travelmate.accommodation.service.analysis.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

/**
 * AI pipeline: comparables -> statistical baseline -> llama3 estimate -> validation -> fallback,
 * then cheaper alternatives with a one-line reason each.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PriceAnalysisServiceImpl implements PriceAnalysisService {

    static final int MAX_ALTERNATIVES = 5;
    static final String SOURCE_AI = "AI";
    static final String SOURCE_STATISTICAL = "STATISTICAL";

    /** Amenities worth mentioning in a fallback reason, most attractive first. */
    private static final List<Amenity> HIGHLIGHT_AMENITIES = List.of(
            Amenity.BREAKFAST, Amenity.POOL, Amenity.SEA_VIEW, Amenity.BEACH_ACCESS,
            Amenity.SPA, Amenity.KITCHEN, Amenity.PARKING, Amenity.GYM);

    private final ComparablesFinder comparablesFinder;
    private final BaselineCalculator baselineCalculator;
    private final PromptBuilder promptBuilder;
    private final OllamaClient ollamaClient;
    private final LlmResponseParser parser;

    @Override
    @Cacheable(cacheNames = CacheConfig.PRICE_ANALYSIS_CACHE,
            key = "#accommodationId + ':' + #checkIn + ':' + #checkOut + ':' + #guests",
            unless = "!#result.aiAvailable()")
    public PriceAnalysisResponse analyze(Long accommodationId, LocalDate checkIn, LocalDate checkOut, Integer guests) {
        validateDates(checkIn, checkOut);
        long started = System.currentTimeMillis();

        // a) comparables + statistical baseline
        AccommodationSummary target = comparablesFinder.loadTarget(accommodationId);
        ComparablesResult comparables = comparablesFinder.findComparables(target);
        BaselineResult baseline = baselineCalculator.compute(target, comparables.comparables());

        // b) + c) LLM estimate, validated, with statistical fallback
        AiResultDto aiResult;
        String aiMessage = null;
        try {
            String prompt = promptBuilder.buildPriceAnalysisPrompt(target, comparables.comparables(), comparables.scope(), baseline);
            OllamaReply reply = ollamaClient.chatJson(PromptBuilder.PRICE_SYSTEM_PROMPT, prompt);
            Optional<ParsedPriceEstimate> parsed = parser.parsePriceEstimate(reply.content(), baseline.baselinePrice(), target.pricePerNight());
            if (parsed.isPresent()) {
                ParsedPriceEstimate p = parsed.get();
                String explanation = p.clamped()
                        ? p.explanation() + " (The AI estimate was capped to stay within ±40% of the market baseline.)"
                        : p.explanation();
                aiResult = new AiResultDto(p.estimatedFairPrice(), p.priceVerdict(), p.differencePercent(),
                        explanation, p.keyFactors().isEmpty() ? statisticalFactors(target, comparables, baseline) : p.keyFactors(),
                        SOURCE_AI, p.clamped(), reply.model(), reply.durationMs());
            } else {
                aiMessage = "The AI answer could not be validated; showing the statistical estimate.";
                aiResult = statisticalResult(target, comparables, baseline);
            }
        } catch (AiUnavailableException e) {
            aiMessage = "Local AI is unavailable right now; showing the statistical estimate.";
            aiResult = statisticalResult(target, comparables, baseline);
        }
        boolean aiAvailable = SOURCE_AI.equals(aiResult.source());

        // d) cheaper alternatives (reasons by AI only if the first call worked, to avoid a second timeout)
        List<AlternativeDto> alternatives = buildAlternatives(target, checkIn, checkOut, guests, aiAvailable);

        log.info("Price analysis for accommodation {} done in {} ms (source={}, comparables={}, alternatives={})",
                accommodationId, System.currentTimeMillis() - started, aiResult.source(),
                baseline.comparablesCount(), alternatives.size());

        return new PriceAnalysisResponse(target, toBaselineDto(target, comparables, baseline), aiResult,
                alternatives, aiAvailable, aiMessage, checkIn, checkOut, Instant.now());
    }

    @Override
    public AiStatusResponse aiStatus() {
        boolean available = ollamaClient.isAvailable();
        if (!available) {
            throw new AiUnavailableException("Local AI (Ollama) is not reachable. Run 'ollama serve' and 'ollama pull "
                    + ollamaClient.model() + "'.");
        }
        return new AiStatusResponse(true, true, ollamaClient.model());
    }

    // ------------------------------------------------------------------ alternatives

    List<AlternativeDto> buildAlternatives(AccommodationSummary target, LocalDate checkIn, LocalDate checkOut,
                                           Integer guests, boolean useAi) {
        List<AccommodationSummary> picked = comparablesFinder.findCheaperCandidates(target, checkIn, checkOut, guests).stream()
                .sorted(Comparator.comparingDouble((AccommodationSummary c) -> SimilarityScorer.score(target, c)).reversed()
                        .thenComparing(AccommodationSummary::pricePerNight))
                .limit(MAX_ALTERNATIVES)
                .toList();
        if (picked.isEmpty()) {
            return List.of();
        }

        Map<Long, String> aiReasons = Map.of();
        if (useAi) {
            try {
                OllamaReply reply = ollamaClient.chatJson(PromptBuilder.ALTERNATIVES_SYSTEM_PROMPT,
                        promptBuilder.buildAlternativesPrompt(target, picked));
                aiReasons = parser.parseReasons(reply.content());
            } catch (AiUnavailableException e) {
                log.info("Alternative reasons fall back to rule-based text: {}", e.getMessage());
            }
        }

        List<AlternativeDto> result = new ArrayList<>();
        for (AccommodationSummary alt : picked) {
            BigDecimal savings = target.pricePerNight().subtract(alt.pricePerNight()).setScale(2, RoundingMode.HALF_UP);
            double savingsPercent = savings.multiply(BigDecimal.valueOf(100))
                    .divide(target.pricePerNight(), 1, RoundingMode.HALF_UP).doubleValue();
            String reason = aiReasons.getOrDefault(alt.id(), fallbackReason(target, alt));
            result.add(new AlternativeDto(alt.id(), alt.name(), alt.type(), alt.city(), alt.stars(), alt.rating(),
                    alt.pricePerNight(), alt.amenities(), alt.mainImageUrl(), savings, savingsPercent,
                    SimilarityScorer.score(target, alt), reason));
        }
        return result;
    }

    /** e.g. "Same city, 4★, rated 4.6, includes breakfast". */
    static String fallbackReason(AccommodationSummary target, AccommodationSummary alt) {
        List<String> parts = new ArrayList<>();
        parts.add(target.city().equalsIgnoreCase(alt.city()) ? "Same city" : "In " + alt.city());
        parts.add(alt.stars() + "★" + (alt.type() == target.type() ? "" : " " + label(alt.type())));
        if (alt.rating() >= target.rating()) {
            parts.add("rated " + alt.rating());
        }
        HIGHLIGHT_AMENITIES.stream()
                .filter(a -> alt.amenities() != null && alt.amenities().contains(a))
                .findFirst()
                .ifPresent(a -> parts.add(amenityPhrase(a)));
        return String.join(", ", parts);
    }

    // ------------------------------------------------------------------ fallback

    AiResultDto statisticalResult(AccommodationSummary target, ComparablesResult comparables, BaselineResult baseline) {
        BigDecimal fair = baseline.baselinePrice();
        double difference = LlmResponseParser.differencePercent(target.pricePerNight(), fair);
        PriceVerdict verdict = PriceVerdict.fromDifference(difference);

        String explanation;
        if (baseline.comparablesCount() == 0) {
            explanation = "No comparable listing was found in " + target.country()
                    + ", so the current price is used as the reference. Add more listings to get a meaningful estimate.";
        } else {
            explanation = String.format(Locale.ROOT,
                    "This estimate is the median nightly price of %d comparable listings %s, adjusted for stars, rating and amenities. "
                            + "At %.2f EUR the listing is %.1f%% %s this baseline, which is considered %s.",
                    baseline.comparablesCount(), scopeLabel(comparables.scope(), target),
                    target.pricePerNight(), Math.abs(difference), difference >= 0 ? "above" : "below",
                    verdict.name().toLowerCase(Locale.ROOT));
        }
        return new AiResultDto(fair, verdict, difference, explanation,
                statisticalFactors(target, comparables, baseline), SOURCE_STATISTICAL, false, null, null);
    }

    static List<String> statisticalFactors(AccommodationSummary target, ComparablesResult comparables, BaselineResult baseline) {
        List<String> factors = new ArrayList<>();
        List<AccommodationSummary> list = comparables.comparables();
        if (list.isEmpty()) {
            factors.add("No comparables found");
            return factors;
        }
        double avgStars = list.stream().mapToInt(AccommodationSummary::stars).average().orElse(0);
        double avgRating = list.stream().mapToDouble(AccommodationSummary::rating).average().orElse(0);
        double avgAmenities = list.stream().mapToInt(c -> c.amenities() == null ? 0 : c.amenities().size()).average().orElse(0);
        factors.add(baseline.comparablesCount() + " comparables " + scopeLabel(comparables.scope(), target));
        factors.add(String.format(Locale.ROOT, "%d★ vs %.1f★ average", target.stars(), avgStars));
        factors.add(String.format(Locale.ROOT, "Rating %.1f vs %.1f average", target.rating(), avgRating));
        factors.add(String.format(Locale.ROOT, "%d amenities vs %.1f average",
                target.amenities() == null ? 0 : target.amenities().size(), avgAmenities));
        return factors;
    }

    // ------------------------------------------------------------------ helpers

    private BaselineDto toBaselineDto(AccommodationSummary target, ComparablesResult comparables, BaselineResult baseline) {
        List<ComparableDto> list = comparables.comparables().stream()
                .map(c -> new ComparableDto(c.id(), c.name(), c.city(), c.type(), c.stars(), c.rating(),
                        c.amenities() == null ? 0 : c.amenities().size(), c.pricePerNight(),
                        SimilarityScorer.score(target, c)))
                .toList();
        return new BaselineDto(baseline.baselinePrice(), baseline.medianComparablePrice(),
                baseline.minComparablePrice(), baseline.maxComparablePrice(),
                baseline.comparablesCount(), comparables.scope(), list);
    }

    private static void validateDates(LocalDate checkIn, LocalDate checkOut) {
        if ((checkIn == null) != (checkOut == null)) {
            throw new InvalidRequestException("checkIn and checkOut must be provided together");
        }
        if (checkIn != null && !checkOut.isAfter(checkIn)) {
            throw new InvalidRequestException("checkOut must be after checkIn");
        }
    }

    private static String scopeLabel(String scope, AccommodationSummary target) {
        return switch (scope) {
            case "CITY" -> "in " + target.city();
            case "COUNTRY" -> "in " + target.country();
            default -> "in " + target.country() + " (all types)";
        };
    }

    private static String label(AccommodationType type) {
        return type.name().charAt(0) + type.name().substring(1).toLowerCase(Locale.ROOT);
    }

    private static String amenityPhrase(Amenity amenity) {
        return switch (amenity) {
            case BREAKFAST -> "includes breakfast";
            case POOL -> "has a pool";
            case SEA_VIEW -> "sea view";
            case BEACH_ACCESS -> "beach access";
            case SPA -> "has a spa";
            case KITCHEN -> "own kitchen";
            case PARKING -> "free parking";
            case GYM -> "has a gym";
            default -> amenity.name().toLowerCase(Locale.ROOT).replace('_', ' ');
        };
    }
}
