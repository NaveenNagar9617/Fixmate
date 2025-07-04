package com.fixmate.dto.analytics;

public record BlockStatsDto(
        String block,
        long count,
        double avgResolutionHours
) {}
