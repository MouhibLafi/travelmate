package com.travelmate.accommodation.ai;

/** Raw text answer of the model plus timing information. */
public record OllamaReply(String content, String model, long durationMs) {
}
