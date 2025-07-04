package com.fixmate.dto.analytics;

import java.time.LocalDate;

public record TrendPointDto(
        LocalDate date,
        long count
) {}
