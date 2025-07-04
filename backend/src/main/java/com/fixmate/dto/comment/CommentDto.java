package com.fixmate.dto.comment;

import java.time.LocalDateTime;
import java.util.UUID;

public record CommentDto(
        UUID id,
        UUID complaintId,
        UUID authorId,
        String authorName,
        String authorRole,
        String content,
        boolean isInternal,
        LocalDateTime createdAt
) {}
