package com.fixmate.dto.notification;

import com.fixmate.model.enums.NotificationType;

import java.time.LocalDateTime;
import java.util.UUID;

public record NotificationDto(
        UUID id,
        NotificationType type,
        String title,
        String message,
        boolean isRead,
        UUID complaintId,
        LocalDateTime createdAt
) {}
