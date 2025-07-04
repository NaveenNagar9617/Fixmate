package com.fixmate.dto.analytics;

public record CategoryBreakdownDto(
        String category,
        long count,
        double percentage
) {}
