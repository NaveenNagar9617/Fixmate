package com.fixmate.model.enums;

import lombok.Getter;

@Getter
public enum Priority {
    CRITICAL(2, "Immediate Escalation"),
    HIGH(4, "Urgent Escalation"),
    MEDIUM(24, "Standard Escalation"),
    LOW(72, "Low Priority Escalation");

    private final int slaHours;
    private final String escalationLabel;

    Priority(int slaHours, String escalationLabel) {
        this.slaHours = slaHours;
        this.escalationLabel = escalationLabel;
    }
}
