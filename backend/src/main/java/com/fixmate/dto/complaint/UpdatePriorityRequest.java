package com.fixmate.dto.complaint;

import com.fixmate.model.enums.Priority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePriorityRequest {

    @NotNull(message = "Priority is required")
    private Priority priority;

    @NotBlank(message = "Reason for priority change is required")
    @Size(max = 500, message = "Reason must not exceed 500 characters")
    private String reason;
}
