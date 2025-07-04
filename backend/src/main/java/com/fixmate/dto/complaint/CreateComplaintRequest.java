package com.fixmate.dto.complaint;

import com.fixmate.model.enums.ComplaintCategory;
import com.fixmate.model.enums.Priority;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateComplaintRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 200, message = "Title must not exceed 200 characters")
    private String title;

    @NotBlank(message = "Description is required")
    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;

    @NotNull(message = "Category is required")
    private ComplaintCategory category;

    @NotNull(message = "Priority is required")
    private Priority priority;

    @NotBlank(message = "Location block is required")
    private String locationBlock;

    @Min(value = 1, message = "Floor must be at least 1")
    private int locationFloor;

    @NotBlank(message = "Room number is required")
    private String roomNumber;

    private java.util.List<String> safetyFlags;

    private String urgencyLevel;
}
