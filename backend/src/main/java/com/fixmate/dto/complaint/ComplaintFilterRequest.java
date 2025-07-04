package com.fixmate.dto.complaint;

import com.fixmate.model.enums.ComplaintCategory;
import com.fixmate.model.enums.ComplaintStatus;
import com.fixmate.model.enums.Priority;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ComplaintFilterRequest {

    private ComplaintStatus status;
    private ComplaintCategory category;
    private Priority priority;
    private String block;
    private Integer floor;
    private LocalDateTime fromDate;
    private LocalDateTime toDate;
    private UUID assignedStaffId;
    private String search;
    private int page = 0;
    private int size = 10;
    private String sort = "createdAt,desc";
}
