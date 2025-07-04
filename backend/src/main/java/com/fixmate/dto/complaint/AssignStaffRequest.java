package com.fixmate.dto.complaint;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssignStaffRequest {

    @NotNull(message = "Staff ID is required")
    private UUID staffId;
}
