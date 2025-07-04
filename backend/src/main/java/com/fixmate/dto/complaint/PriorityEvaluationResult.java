package com.fixmate.dto.complaint;

import com.fixmate.model.enums.Priority;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PriorityEvaluationResult {
    private Priority requestedPriority;
    private Priority evaluatedPriority;
    private String reason;
    private boolean emergencyDetected;
    private double confidence;
    private List<String> matchedIndicators;
}
