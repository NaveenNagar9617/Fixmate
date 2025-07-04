package com.fixmate.dto.complaint;

import com.fixmate.model.enums.ComplaintStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateStatusRequest {

    @NotNull(message = "New status is required")
    private ComplaintStatus newStatus;

    private String note;
}
