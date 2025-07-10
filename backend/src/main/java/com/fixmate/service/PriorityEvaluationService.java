package com.fixmate.service;

import com.fixmate.dto.complaint.PriorityEvaluationResult;
import com.fixmate.model.enums.ComplaintCategory;
import com.fixmate.model.enums.Priority;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class PriorityEvaluationService {

    // Emergency life-safety indicators -> CRITICAL (2h SLA)
    private static final List<String> EMERGENCY_KEYWORDS = List.of(
            "fire", "sparking", "short circuit", "smoke", "electric shock", "shock",
            "gas leak", "burst pipe", "flooding", "flood", "theft", "break in",
            "trapped", "hazard", "danger", "collapse", "ceiling falling"
    );

    // High operational disruption indicators -> HIGH (4h SLA)
    private static final List<String> HIGH_URGENCY_KEYWORDS = List.of(
            "no water", "water cut", "blackout", "power outage", "no power", "no electricity",
            "door jammed", "door lock broken", "broken window", "toilet choked", "choked",
            "overflowing", "sewage", "entire floor", "whole block", "all rooms"
    );

    // Routine / minor indicators -> LOW or MEDIUM
    private static final List<String> ROUTINE_KEYWORDS = List.of(
            "fan speed", "flicker", "paint", "scratched", "scratch", "creak", "bulb",
            "dustbin", "broom", "mirror", "chair", "table", "curtain", "loose", "dirty", "slow"
    );

    private static final Set<String> SAFETY_FLAGS_SET = Set.of(
            "SPARKING", "SMOKE", "GAS_LEAK", "FLOOD_NEAR_ELECTRIC", "PERSON_TRAPPED", "STRUCTURAL_DAMAGE"
    );

    public PriorityEvaluationResult evaluate(
            ComplaintCategory category,
            String title,
            String description,
            Priority requestedPriority,
            List<String> safetyFlags,
            String urgencyLevel
    ) {
        String fullText = ((title != null ? title : "") + " " + (description != null ? description : "")).toLowerCase();
        List<String> matchedIndicators = new ArrayList<>();

        // 1. Check for Structured Safety Flags
        boolean hasSafetyFlags = false;
        if (safetyFlags != null) {
            for (String flag : safetyFlags) {
                if (flag != null && SAFETY_FLAGS_SET.contains(flag.toUpperCase())) {
                    matchedIndicators.add("FLAG:" + flag);
                    hasSafetyFlags = true;
                }
            }
        }

        // 2. Check for Emergency Hazard Keywords
        for (String kw : EMERGENCY_KEYWORDS) {
            if (containsWordOrPhrase(fullText, kw)) {
                matchedIndicators.add(kw);
            }
        }

        if (hasSafetyFlags || !matchedIndicators.isEmpty()) {
            log.info("Emergency indicators detected: {}. Elevating to CRITICAL", matchedIndicators);
            return PriorityEvaluationResult.builder()
                    .requestedPriority(requestedPriority)
                    .evaluatedPriority(Priority.CRITICAL)
                    .emergencyDetected(true)
                    .confidence(0.98)
                    .matchedIndicators(matchedIndicators)
                    .reason("Immediate safety/emergency indicator detected: " + String.join(", ", matchedIndicators))
                    .build();
        }

        // 3. Check for High Operational Disruption Keywords
        List<String> urgentMatches = new ArrayList<>();
        for (String kw : HIGH_URGENCY_KEYWORDS) {
            if (containsWordOrPhrase(fullText, kw)) {
                urgentMatches.add(kw);
            }
        }

        if (!urgentMatches.isEmpty()) {
            log.info("High urgency indicators detected: {}. Setting to HIGH", urgentMatches);
            return PriorityEvaluationResult.builder()
                    .requestedPriority(requestedPriority)
                    .evaluatedPriority(Priority.HIGH)
                    .emergencyDetected(false)
                    .confidence(0.90)
                    .matchedIndicators(urgentMatches)
                    .reason("High operational disruption detected: " + String.join(", ", urgentMatches))
                    .build();
        }

        // 4. Category Baseline Priority
        Priority baseline = getCategoryBaseline(category);

        // 5. Check Scope / Urgency Level Modifier
        if ("FLOOR".equalsIgnoreCase(urgencyLevel) || "BUILDING".equalsIgnoreCase(urgencyLevel)
                || fullText.contains("entire floor") || fullText.contains("whole block")) {
            if (baseline == Priority.LOW) {
                baseline = Priority.MEDIUM;
            } else if (baseline == Priority.MEDIUM) {
                baseline = Priority.HIGH;
            }
        }

        // 6. Routine Keyword Guardrail Capping
        List<String> routineMatches = new ArrayList<>();
        for (String kw : ROUTINE_KEYWORDS) {
            if (containsWordOrPhrase(fullText, kw)) {
                routineMatches.add(kw);
            }
        }

        Priority effectivePriority;
        String reason;

        if (requestedPriority == Priority.CRITICAL && !routineMatches.isEmpty()) {
            // Student claimed CRITICAL on a routine maintenance task
            effectivePriority = (baseline == Priority.HIGH) ? Priority.MEDIUM : Priority.LOW;
            reason = "Normalized from CRITICAL to " + effectivePriority + ": Routine maintenance indicators (" +
                    String.join(", ", routineMatches) + ") without verified safety hazards";
        } else if (requestedPriority == Priority.CRITICAL) {
            // No emergency keywords matched, cap to HIGH
            effectivePriority = Priority.HIGH;
            reason = "Student requested CRITICAL adjusted to HIGH: High priority assigned pending staff verification";
        } else if (requestedPriority == Priority.HIGH && isLowSeverityCategory(category) && !routineMatches.isEmpty()) {
            effectivePriority = Priority.LOW;
            reason = "Normalized from HIGH to LOW: Routine " + category.name().toLowerCase() + " maintenance request";
        } else if (requestedPriority != null) {
            // Normal matching
            effectivePriority = requestedPriority;
            reason = "Priority accepted based on category standard and request";
        } else {
            effectivePriority = baseline;
            reason = "Priority evaluated based on category baseline (" + baseline + ")";
        }

        return PriorityEvaluationResult.builder()
                .requestedPriority(requestedPriority)
                .evaluatedPriority(effectivePriority)
                .emergencyDetected(false)
                .confidence(0.85)
                .matchedIndicators(routineMatches)
                .reason(reason)
                .build();
    }

    private Priority getCategoryBaseline(ComplaintCategory category) {
        if (category == null) return Priority.LOW;
        return switch (category) {
            case ELECTRICAL, SECURITY -> Priority.HIGH;
            case PLUMBING, WIFI -> Priority.MEDIUM;
            case FURNITURE, CLEANING, PEST_CONTROL, OTHER -> Priority.LOW;
        };
    }

    private boolean isLowSeverityCategory(ComplaintCategory category) {
        return category == ComplaintCategory.FURNITURE ||
                category == ComplaintCategory.CLEANING ||
                category == ComplaintCategory.PEST_CONTROL ||
                category == ComplaintCategory.OTHER;
    }

    private boolean containsWordOrPhrase(String text, String phrase) {
        if (text == null || phrase == null || text.isBlank() || phrase.isBlank()) return false;
        String patternString = "\\b" + Pattern.quote(phrase.toLowerCase()) + "\\b";
        Pattern pattern = Pattern.compile(patternString, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(text);
        return matcher.find();
    }
}
