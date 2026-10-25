package com.travelmate.accommodation.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travelmate.accommodation.dto.PriceVerdict;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class LlmResponseParserTest {

    private static final BigDecimal BASELINE = new BigDecimal("150.00");
    private static final BigDecimal CURRENT = new BigDecimal("180.00");

    private final LlmResponseParser parser = new LlmResponseParser(new ObjectMapper());

    @Test
    void parsesValidAnswerAndRecomputesDifference() {
        String raw = """
                {"estimatedFairPrice": 160, "priceVerdict": "FAIR", "differencePercent": 3,
                 "explanation": "Similar hotels cost 150-170 EUR.", "keyFactors": ["Central location", "Breakfast included"]}
                """;

        ParsedPriceEstimate p = parser.parsePriceEstimate(raw, BASELINE, CURRENT).orElseThrow();

        assertThat(p.estimatedFairPrice()).isEqualByComparingTo("160.00");
        assertThat(p.differencePercent()).isEqualTo(12.5);               // (180-160)/160, not the model's 3
        assertThat(p.priceVerdict()).isEqualTo(PriceVerdict.OVERPRICED); // recomputed, not the model's FAIR
        assertThat(p.clamped()).isFalse();
        assertThat(p.keyFactors()).containsExactly("Central location", "Breakfast included");
    }

    @Test
    void clampsTooHighEstimateToBaselinePlus40Percent() {
        ParsedPriceEstimate p = parser.parsePriceEstimate("{\"estimatedFairPrice\": 900}", BASELINE, CURRENT).orElseThrow();

        assertThat(p.estimatedFairPrice()).isEqualByComparingTo("210.00");
        assertThat(p.clamped()).isTrue();
        assertThat(p.priceVerdict()).isEqualTo(PriceVerdict.UNDERPRICED);
    }

    @Test
    void clampsTooLowEstimateToBaselineMinus40Percent() {
        ParsedPriceEstimate p = parser.parsePriceEstimate("{\"estimatedFairPrice\": 10}", BASELINE, CURRENT).orElseThrow();

        assertThat(p.estimatedFairPrice()).isEqualByComparingTo("90.00");
        assertThat(p.clamped()).isTrue();
    }

    @Test
    void toleratesMarkdownFencesAndNumericStrings() {
        String raw = "Here is the result:\n```json\n{\"estimatedFairPrice\": \"155.5 EUR\", \"explanation\": \"ok\"}\n```";

        ParsedPriceEstimate p = parser.parsePriceEstimate(raw, BASELINE, CURRENT).orElseThrow();

        assertThat(p.estimatedFairPrice()).isEqualByComparingTo("155.50");
        assertThat(p.keyFactors()).isEmpty();
    }

    @Test
    void blankExplanationGetsDefaultText() {
        ParsedPriceEstimate p = parser.parsePriceEstimate("{\"estimatedFairPrice\": 150, \"explanation\": \"  \"}", BASELINE, CURRENT)
                .orElseThrow();
        assertThat(p.explanation()).isNotBlank();
    }

    @Test
    void limitsKeyFactors() {
        String raw = "{\"estimatedFairPrice\": 150, \"keyFactors\": [\"a\",\"b\",\"c\",\"d\",\"e\",\"f\",\"g\"]}";
        assertThat(parser.parsePriceEstimate(raw, BASELINE, CURRENT).orElseThrow().keyFactors()).hasSize(5);
    }

    @Test
    void rejectsUnusableAnswers() {
        assertThat(parser.parsePriceEstimate("I think it is around 150 euros", BASELINE, CURRENT)).isEmpty();
        assertThat(parser.parsePriceEstimate("{\"estimatedFairPrice\": ", BASELINE, CURRENT)).isEmpty();
        assertThat(parser.parsePriceEstimate("{\"priceVerdict\": \"FAIR\"}", BASELINE, CURRENT)).isEmpty();
        assertThat(parser.parsePriceEstimate("{\"estimatedFairPrice\": -5}", BASELINE, CURRENT)).isEmpty();
        assertThat(parser.parsePriceEstimate("{\"estimatedFairPrice\": \"cheap\"}", BASELINE, CURRENT)).isEmpty();
        assertThat(parser.parsePriceEstimate("", BASELINE, CURRENT)).isEmpty();
        assertThat(parser.parsePriceEstimate(null, BASELINE, CURRENT)).isEmpty();
    }

    @Test
    void parsesReasonsInBothFormats() {
        Map<Long, String> arrayFormat = parser.parseReasons(
                "{\"reasons\": [{\"id\": 3, \"reason\": \"Same area, 4 stars\"}, {\"id\": \"x\", \"reason\": \"bad id\"}]}");
        Map<Long, String> mapFormat = parser.parseReasons("{\"3\": \"Same area\", \"7\": \"Has a pool\"}");

        assertThat(arrayFormat).containsExactly(Map.entry(3L, "Same area, 4 stars"));
        assertThat(mapFormat).containsEntry(3L, "Same area").containsEntry(7L, "Has a pool");
        assertThat(parser.parseReasons("not json")).isEmpty();
    }

    @Test
    void verdictThresholds() {
        assertThat(PriceVerdict.fromDifference(10.0)).isEqualTo(PriceVerdict.FAIR);
        assertThat(PriceVerdict.fromDifference(10.1)).isEqualTo(PriceVerdict.OVERPRICED);
        assertThat(PriceVerdict.fromDifference(-10.1)).isEqualTo(PriceVerdict.UNDERPRICED);
        assertThat(Optional.of(LlmResponseParser.differencePercent(new BigDecimal("90"), new BigDecimal("100"))))
                .contains(-10.0);
    }
}
