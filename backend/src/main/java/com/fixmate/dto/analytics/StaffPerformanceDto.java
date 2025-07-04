package com.fixmate.dto.analytics;

import java.util.UUID;

public record StaffPerformanceDto(
        UUID staffId,
        String name,
        long assigned,
        long resolved,
        double avgHours,
        long reopenCount,
        double avgRating
) {}
