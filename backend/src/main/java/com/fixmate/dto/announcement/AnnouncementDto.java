package com.fixmate.dto.announcement;

import com.fixmate.model.enums.Audience;

import java.time.LocalDateTime;
import java.util.UUID;

public record AnnouncementDto(
        UUID id,
        String adminName,
        String title,
        String content,
        Audience targetAudience,
        LocalDateTime expiresAt,
        LocalDateTime createdAt
) {}
