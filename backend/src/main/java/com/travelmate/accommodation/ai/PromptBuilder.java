package com.travelmate.accommodation.ai;

import com.travelmate.accommodation.dto.AccommodationSummary;
import com.travelmate.accommodation.entity.Amenity;
import com.travelmate.accommodation.service.analysis.BaselineResult;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/** Builds the prompts sent to llama3. Kept deterministic and compact so answers are fast and stable. */
@Component
public class PromptBuilder {

    public static final String PRICE_SYSTEM_PROMPT = """
            You are a hotel revenue analyst. You estimate the fair nightly price of a listing \
            by comparing it with similar listings in the same market. \
            You always answer with ONLY one valid JSON object, no markdown, no text before or after.""";

    public static final String ALTERNATIVES_SYSTEM_PROMPT = """
            You are a helpful travel assistant. You explain in a few words why a listing is a good cheaper \
            alternative. You always answer with ONLY one valid JSON object, no markdown.""";

    public String buildPriceAnalysisPrompt(AccommodationSummary target, List<AccommodationSummary> comparables,
                                           String scope, BaselineResult baseline) {
        StringBuilder sb = new StringBuilder();
        sb.append("TARGET LISTING\n")
                .append("- Name: ").append(target.name()).append('\n')
                .append("- Type: ").append(target.type())
                .append(" | Location: ").append(target.city()).append(", ").append(target.country()).append('\n')
                .append("- Stars: ").append(target.stars())
                .append(" | Guest rating: ").append(target.rating()).append("/5")
                .append(" | Max guests: ").append(target.maxGuests()).append('\n')
                .append("- Amenities (").append(size(target)).append("): ").append(amenities(target)).append('\n')
                .append("- Current price per night: ").append(eur(target.pricePerNight())).append("\n\n");

        sb.append("COMPARABLE LISTINGS (").append(describeScope(scope, target)).append(") - ")
                .append(comparables.size()).append(" found\n");
        if (comparables.isEmpty()) {
            sb.append("(none - rely on the baseline and general market knowledge)\n");
        }
        for (int i = 0; i < comparables.size(); i++) {
            AccommodationSummary c = comparables.get(i);
            sb.append(i + 1).append(". ").append(c.name())
                    .append(" | ").append(c.type()).append(", ").append(c.city())
                    .append(" | ").append(c.stars()).append(" stars")
                    .append(" | rating ").append(c.rating())
                    .append(" | amenities: ").append(amenities(c))
                    .append(" | ").append(eur(c.pricePerNight())).append("/night\n");
        }

        sb.append("\nSTATISTICAL BASELINE\n");
        if (baseline.medianComparablePrice() != null) {
            sb.append("- Median comparable price: ").append(eur(baseline.medianComparablePrice())).append('\n');
        }
        sb.append("- Baseline adjusted for stars, rating and amenities: ").append(eur(baseline.baselinePrice())).append("\n\n");

        sb.append("""
                TASK
                Estimate the fair price per night of the TARGET listing in EUR.
                Stay close to the baseline unless the target's stars, rating, amenities or location clearly justify a difference.
                differencePercent = (current price - estimatedFairPrice) / estimatedFairPrice * 100.
                priceVerdict: OVERPRICED if differencePercent > 10, UNDERPRICED if differencePercent < -10, otherwise FAIR.
                The explanation must be 2 or 3 sentences, written for a traveller, citing concrete comparables or features.
                keyFactors: 2 to 5 short factors (max 6 words each).

                Return ONLY this JSON:
                {"estimatedFairPrice": number, "priceVerdict": "UNDERPRICED" | "FAIR" | "OVERPRICED", "differencePercent": number, "explanation": "string", "keyFactors": ["string"]}
                """);
        return sb.toString();
    }

    public String buildAlternativesPrompt(AccommodationSummary target, List<AccommodationSummary> alternatives) {
        StringBuilder sb = new StringBuilder();
        sb.append("TARGET: ").append(target.name()).append(" | ").append(target.type()).append(", ").append(target.city())
                .append(" | ").append(target.stars()).append(" stars | rating ").append(target.rating())
                .append(" | amenities: ").append(amenities(target))
                .append(" | ").append(eur(target.pricePerNight())).append("/night\n\n");
        sb.append("CHEAPER ALTERNATIVES\n");
        for (AccommodationSummary a : alternatives) {
            sb.append("id=").append(a.id()).append(" | ").append(a.name())
                    .append(" | ").append(a.type()).append(", ").append(a.city())
                    .append(" | ").append(a.stars()).append(" stars | rating ").append(a.rating())
                    .append(" | amenities: ").append(amenities(a))
                    .append(" | ").append(eur(a.pricePerNight())).append("/night\n");
        }
        sb.append("""

                For EACH alternative write ONE short reason (max 12 words) why it is a good cheaper option \
                compared to the target, mentioning concrete shared features, e.g. "Same area, 4 stars, includes breakfast".
                Return ONLY this JSON:
                {"reasons": [{"id": number, "reason": "string"}]}
                """);
        return sb.toString();
    }

    private static String describeScope(String scope, AccommodationSummary target) {
        return switch (scope) {
            case "CITY" -> "same city, same type, stars +/-1";
            case "COUNTRY" -> "same country (" + target.country() + "), same type, stars +/-1";
            default -> "same country (" + target.country() + "), any type, stars +/-1";
        };
    }

    private static int size(AccommodationSummary a) {
        return a.amenities() == null ? 0 : a.amenities().size();
    }

    private static String amenities(AccommodationSummary a) {
        if (a.amenities() == null || a.amenities().isEmpty()) {
            return "none";
        }
        return a.amenities().stream().map(Amenity::name).sorted().collect(Collectors.joining(", "));
    }

    private static String eur(BigDecimal value) {
        return String.format(Locale.ROOT, "%.2f EUR", value);
    }
}
