package com.fixmate.dto.complaint;

import com.fixmate.model.enums.ComplaintCategory;
import com.fixmate.model.enums.ComplaintStatus;
import com.fixmate.model.enums.Priority;

import java.time.LocalDateTime;
import java.util.UUID;

public record ComplaintListDto(
        UUID id,
        String title,
        ComplaintCategory category,
        Priority priority,
        ComplaintStatus status,
        String locationBlock,
        int locationFloor,
        String roomNumber,
        String studentName,
        UUID studentId,
        String assignedStaffName,
        UUID assignedStaffId,
        int upvoteCount,
        LocalDateTime slaDeadline,
        String slaStatus,
        LocalDateTime createdAt,
        LocalDateTime resolvedAt
) {}
