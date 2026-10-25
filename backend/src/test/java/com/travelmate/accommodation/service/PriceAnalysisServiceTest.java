package com.travelmate.accommodation.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travelmate.accommodation.ai.LlmResponseParser;
import com.travelmate.accommodation.ai.OllamaClient;
import com.travelmate.accommodation.ai.OllamaReply;
import com.travelmate.accommodation.ai.PromptBuilder;
import com.travelmate.accommodation.dto.AccommodationSummary;
import com.travelmate.accommodation.dto.PriceAnalysisResponse;
import com.travelmate.accommodation.dto.PriceVerdict;
import com.travelmate.accommodation.exception.AiUnavailableException;
import com.travelmate.accommodation.exception.InvalidRequestException;
import com.travelmate.accommodation.service.analysis.BaselineCalculator;
import com.travelmate.accommodation.service.analysis.ComparablesFinder;
import com.travelmate.accommodation.service.analysis.ComparablesResult;
import com.travelmate.accommodation.service.impl.PriceAnalysisServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static com.travelmate.accommodation.entity.AccommodationType.HOTEL;
import static com.travelmate.accommodation.entity.Amenity.*;
import static com.travelmate.accommodation.support.TestData.summary;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Pipeline test with a mocked OllamaClient and real baseline / prompt / parser components. */
@ExtendWith(MockitoExtension.class)
class PriceAnalysisServiceTest {

    @Mock
    private ComparablesFinder finder;
    @Mock
    private OllamaClient ollama;

    private PriceAnalysisService service;

    private final AccommodationSummary target = summary(1, "Rome", HOTEL, 4, 4.5, 185, WIFI, BREAKFAST, GYM);
    private final List<AccommodationSummary> comparables = List.of(
            summary(2, "Rome", HOTEL, 4, 4.5, 160, WIFI, BREAKFAST, GYM),
            summary(3, "Rome", HOTEL, 4, 4.5, 170, WIFI, BREAKFAST, GYM),
            summary(4, "Rome", HOTEL, 4, 4.5, 180, WIFI, BREAKFAST, GYM));   // baseline = 170
    private final List<AccommodationSummary> cheaper = List.of(
            summary(5, "Rome", HOTEL, 4, 4.6, 150, WIFI, BREAKFAST),
            summary(6, "Rome", HOTEL, 2, 3.5, 60, WIFI),
            summary(7, "Milan", HOTEL, 4, 4.5, 120, WIFI, POOL));

    @BeforeEach
    void setUp() {
        service = new PriceAnalysisServiceImpl(finder, new BaselineCalculator(), new PromptBuilder(), ollama,
                new LlmResponseParser(new ObjectMapper()));
        lenient().when(finder.loadTarget(1L)).thenReturn(target);
        lenient().when(finder.findComparables(target)).thenReturn(new ComparablesResult("CITY", comparables));
        lenient().when(finder.findCheaperCandidates(eq(target), any(), any(), any())).thenReturn(cheaper);
    }

    @Test
    void usesValidatedAiAnswerWhenOllamaWorks() {
        when(ollama.chatJson(eq(PromptBuilder.PRICE_SYSTEM_PROMPT), anyString())).thenReturn(new OllamaReply("""
                {"estimatedFairPrice": 175, "priceVerdict": "OVERPRICED", "differencePercent": 40,
                 "explanation": "Comparable hotels cost 160-180 EUR.", "keyFactors": ["Gym", "Breakfast"]}""", "llama3", 1200));
        when(ollama.chatJson(eq(PromptBuilder.ALTERNATIVES_SYSTEM_PROMPT), anyString()))
                .thenReturn(new OllamaReply("{\"reasons\":[{\"id\":5,\"reason\":\"Same area, 4★, breakfast\"}]}", "llama3", 300));

        PriceAnalysisResponse r = service.analyze(1L, null, null, null);

        assertThat(r.aiAvailable()).isTrue();
        assertThat(r.aiMessage()).isNull();
        assertThat(r.baseline().baselinePrice()).isEqualByComparingTo("170.00");
        assertThat(r.aiResult().source()).isEqualTo("AI");
        assertThat(r.aiResult().estimatedFairPrice()).isEqualByComparingTo("175.00");
        assertThat(r.aiResult().differencePercent()).isEqualTo(5.7);           // recomputed: (185-175)/175
        assertThat(r.aiResult().priceVerdict()).isEqualTo(PriceVerdict.FAIR);   // model said OVERPRICED
        assertThat(r.aiResult().keyFactors()).containsExactly("Gym", "Breakfast");

        assertThat(r.alternatives()).extracting("id").containsExactly(5L, 6L, 7L); // ranked by similarity (same city weighs 30%)
        assertThat(r.alternatives().get(0).reason()).isEqualTo("Same area, 4★, breakfast");
        assertThat(r.alternatives().get(0).savingsPerNight()).isEqualByComparingTo("35.00");
        assertThat(r.alternatives().get(1).reason()).isNotBlank();              // missing AI reason -> rule-based
    }

    @Test
    void fallsBackToBaselineWhenOllamaIsDown() {
        when(ollama.chatJson(anyString(), anyString())).thenThrow(new AiUnavailableException("connection refused"));

        PriceAnalysisResponse r = service.analyze(1L, null, null, null);

        assertThat(r.aiAvailable()).isFalse();
        assertThat(r.aiMessage()).contains("unavailable");
        assertThat(r.aiResult().source()).isEqualTo("STATISTICAL");
        assertThat(r.aiResult().estimatedFairPrice()).isEqualByComparingTo("170.00");
        assertThat(r.aiResult().differencePercent()).isEqualTo(8.8);
        assertThat(r.aiResult().priceVerdict()).isEqualTo(PriceVerdict.FAIR);
        assertThat(r.aiResult().explanation()).contains("3 comparable listings");
        assertThat(r.alternatives()).hasSize(3).allSatisfy(a -> assertThat(a.reason()).isNotBlank());
        assertThat(r.alternatives().get(0).reason()).isEqualTo("Same city, 4★, rated 4.6, includes breakfast");
        // the reasons call is skipped once the AI failed, so no second timeout
        verify(ollama, times(1)).chatJson(anyString(), anyString());
    }

    @Test
    void fallsBackWhenAnswerIsInvalidJson() {
        when(ollama.chatJson(eq(PromptBuilder.PRICE_SYSTEM_PROMPT), anyString()))
                .thenReturn(new OllamaReply("Sorry, I cannot help with that.", "llama3", 900));

        PriceAnalysisResponse r = service.analyze(1L, null, null, null);

        assertThat(r.aiAvailable()).isFalse();
        assertThat(r.aiMessage()).contains("could not be validated");
        assertThat(r.aiResult().estimatedFairPrice()).isEqualByComparingTo("170.00");
    }

    @Test
    void clampsAbsurdAiEstimate() {
        when(ollama.chatJson(eq(PromptBuilder.PRICE_SYSTEM_PROMPT), anyString()))
                .thenReturn(new OllamaReply("{\"estimatedFairPrice\": 5000, \"explanation\": \"Luxury.\"}", "llama3", 900));
        when(ollama.chatJson(eq(PromptBuilder.ALTERNATIVES_SYSTEM_PROMPT), anyString()))
                .thenThrow(new AiUnavailableException("timeout"));

        PriceAnalysisResponse r = service.analyze(1L, null, null, null);

        assertThat(r.aiAvailable()).isTrue();
        assertThat(r.aiResult().clamped()).isTrue();
        assertThat(r.aiResult().estimatedFairPrice()).isEqualByComparingTo("238.00"); // 170 x 1.4
        assertThat(r.aiResult().priceVerdict()).isEqualTo(PriceVerdict.UNDERPRICED);
        assertThat(r.alternatives()).allSatisfy(a -> assertThat(a.reason()).isNotBlank()); // reasons fallback
    }

    @Test
    void passesDatesAndGuestsToAlternativeSearch() {
        when(ollama.chatJson(anyString(), anyString())).thenThrow(new AiUnavailableException("down"));
        LocalDate in = LocalDate.now().plusDays(10);

        service.analyze(1L, in, in.plusDays(2), 2);

        verify(finder).findCheaperCandidates(target, in, in.plusDays(2), 2);
    }

    @Test
    void rejectsIncompleteDateRange() {
        assertThatThrownBy(() -> service.analyze(1L, LocalDate.now(), null, null))
                .isInstanceOf(InvalidRequestException.class);
    }
}
