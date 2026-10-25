package com.travelmate.accommodation.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

/** Caffeine cache, configured by spring.cache.* in application.properties. */
@Configuration
@EnableCaching
public class CacheConfig {
    public static final String PRICE_ANALYSIS_CACHE = "priceAnalysis";
}
