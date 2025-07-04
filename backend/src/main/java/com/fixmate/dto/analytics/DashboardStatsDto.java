package com.fixmate.dto.analytics;

public record DashboardStatsDto(
        long openCount,
        long closedCount,
        long escalatedCount,
        double slaBreachRate,
        double avgResolutionHours,
        double reopenRate,
        long totalComplaints
) {}
