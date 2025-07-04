package com.fixmate.dto.complaint;

import com.fixmate.dto.comment.CommentDto;
import com.fixmate.dto.user.UserSummaryDto;
import com.fixmate.model.enums.ComplaintCategory;
import com.fixmate.model.enums.ComplaintStatus;
import com.fixmate.model.enums.Priority;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record ComplaintDetailDto(
        UUID id,
        String title,
        String description,
        ComplaintCategory category,
        Priority priority,
        Priority requestedPriority,
        String priorityReason,
        String prioritySource,
        LocalDateTime priorityUpdatedAt,
        ComplaintStatus status,
        String locationBlock,
        int locationFloor,
        String roomNumber,
        UserSummaryDto student,
        UserSummaryDto assignedStaff,
        UUID parentComplaintId,
        LocalDateTime slaDeadline,
        String slaStatus,
        LocalDateTime resolvedAt,
        int reopenedCount,
        int upvoteCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<PhotoDto> photos,
        List<TimelineDto> timeline,
        List<CommentDto> comments,
        boolean hasRating,
        boolean userHasUpvoted
) {
    public record PhotoDto(
            UUID id,
            String photoUrl,
            String photoType,
            String uploadedByName,
            LocalDateTime uploadedAt
    ) {}

    public record TimelineDto(
            UUID id,
            String actorName,
            String action,
            String oldStatus,
            String newStatus,
            String note,
            LocalDateTime createdAt
    ) {}
}
