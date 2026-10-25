package com.travelmate.accommodation.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.travelmate.accommodation.dto.PriceVerdict;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * Never trust the LLM: this class parses its JSON defensively and enforces business rules.
 * <ul>
 *   <li>estimatedFairPrice must be a positive number, then is clamped to baseline +/-40%;</li>
 *   <li>differencePercent and priceVerdict are recomputed from the clamped price (the model's values are ignored);</li>
 *   <li>explanation and keyFactors are trimmed and length-limited.</li>
 * </ul>
 */
@Slf4j
@Component
public class LlmResponseParser {

    public static final BigDecimal MAX_DEVIATION = new BigDecimal("0.40");
    static final int MAX_EXPLANATION_LENGTH = 600;
    static final int MAX_FACTORS = 5;
    static final int MAX_FACTOR_LENGTH = 60;
    static final int MAX_REASON_LENGTH = 120;

    private final ObjectMapper objectMapper;

    public LlmResponseParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /** @return the validated estimate, or empty if the answer is not usable (caller falls back to the baseline). */
    public Optional<ParsedPriceEstimate> parsePriceEstimate(String raw, BigDecimal baseline, BigDecimal currentPrice) {
        JsonNode root = readJson(raw).orElse(null);
        if (root == null) {
            return Optional.empty();
        }
        JsonNode priceNode = root.get("estimatedFairPrice");
        BigDecimal estimate = toNumber(priceNode);
        if (estimate == null || estimate.signum() <= 0) {
            log.warn("LLM answer has no valid estimatedFairPrice: {}", abbreviate(raw));
            return Optional.empty();
        }

        BigDecimal min = baseline.multiply(BigDecimal.ONE.subtract(MAX_DEVIATION));
        BigDecimal max = baseline.multiply(BigDecimal.ONE.add(MAX_DEVIATION));
        boolean clamped = false;
        if (estimate.compareTo(min) < 0) {
            estimate = min;
            clamped = true;
        } else if (estimate.compareTo(max) > 0) {
            estimate = max;
            clamped = true;
        }
        if (clamped) {
            log.info("LLM estimate {} clamped into [{}, {}]", priceNode, min, max);
        }
        estimate = estimate.setScale(2, RoundingMode.HALF_UP);

        double difference = differencePercent(currentPrice, estimate);
        String explanation = text(root.get("explanation"));
        if (explanation.isBlank()) {
            explanation = "Estimate based on comparable listings in the same market.";
        }
        explanation = truncate(explanation, MAX_EXPLANATION_LENGTH);

        return Optional.of(new ParsedPriceEstimate(estimate, PriceVerdict.fromDifference(difference), difference,
                explanation, keyFactors(root.get("keyFactors")), clamped));
    }

    /** Accepts {"reasons":[{"id":1,"reason":"..."}]} or {"1":"...", "2":"..."}. Unknown ids are ignored by the caller. */
    public Map<Long, String> parseReasons(String raw) {
        Map<Long, String> reasons = new HashMap<>();
        JsonNode root = readJson(raw).orElse(null);
        if (root == null) {
            return reasons;
        }
        JsonNode list = root.has("reasons") ? root.get("reasons") : root;
        if (list.isArray()) {
            for (JsonNode item : list) {
                BigDecimal id = toNumber(item.get("id"));
                String reason = text(item.get("reason"));
                if (id != null && !reason.isBlank()) {
                    reasons.put(id.longValue(), truncate(reason, MAX_REASON_LENGTH));
                }
            }
        } else if (list.isObject()) {
            list.fields().forEachRemaining(e -> {
                try {
                    String reason = text(e.getValue());
                    if (!reason.isBlank()) {
                        reasons.put(Long.parseLong(e.getKey().trim()), truncate(reason, MAX_REASON_LENGTH));
                    }
                } catch (NumberFormatException ignored) {
                    // not an id key
                }
            });
        }
        return reasons;
    }

    /** (current - fair) / fair * 100, rounded to one decimal. */
    public static double differencePercent(BigDecimal currentPrice, BigDecimal fairPrice) {
        return currentPrice.subtract(fairPrice)
                .multiply(BigDecimal.valueOf(100))
                .divide(fairPrice, 1, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private Optional<JsonNode> readJson(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        String text = raw.trim();
        // Models sometimes wrap JSON in ```json fences or add a sentence around it.
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start < 0 || end <= start) {
            log.warn("LLM answer is not JSON: {}", abbreviate(raw));
            return Optional.empty();
        }
        try {
            JsonNode node = objectMapper.readTree(text.substring(start, end + 1));
            return node != null && node.isObject() ? Optional.of(node) : Optional.empty();
        } catch (Exception e) {
            log.warn("LLM answer is invalid JSON ({}): {}", e.getMessage(), abbreviate(raw));
            return Optional.empty();
        }
    }

    private static BigDecimal toNumber(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isNumber()) {
            return node.decimalValue();
        }
        if (node.isTextual()) {
            String cleaned = node.asText().replaceAll("[^0-9.,-]", "").replace(',', '.');
            try {
                return cleaned.isEmpty() ? null : new BigDecimal(cleaned);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    private static List<String> keyFactors(JsonNode node) {
        List<String> factors = new ArrayList<>();
        if (node != null && node.isArray()) {
            for (JsonNode f : node) {
                String value = text(f);
                if (!value.isBlank() && factors.size() < MAX_FACTORS) {
                    factors.add(truncate(value, MAX_FACTOR_LENGTH));
                }
            }
        }
        return factors;
    }

    private static String text(JsonNode node) {
        return node == null || node.isNull() || node.isContainerNode() ? "" : node.asText().trim();
    }

    private static String truncate(String value, int max) {
        return value.length() <= max ? value : value.substring(0, max - 1).trim() + "…";
    }

    private static String abbreviate(String raw) {
        return raw == null ? "null" : truncate(raw.replaceAll("\\s+", " "), 200);
    }
}
