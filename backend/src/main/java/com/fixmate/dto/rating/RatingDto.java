package com.fixmate.dto.rating;

import java.time.LocalDateTime;
import java.util.UUID;

public record RatingDto(
        UUID id,
        UUID complaintId,
        String complaintTitle,
        UUID studentId,
        String studentName,
        int stars,
        String feedbackText,
        LocalDateTime createdAt
) {}
